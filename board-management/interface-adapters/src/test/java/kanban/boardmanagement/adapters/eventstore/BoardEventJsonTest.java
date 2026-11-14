package kanban.boardmanagement.adapters.eventstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import kanban.boardmanagement.entities.board.BoardArchived;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.entities.board.BoardRenamed;
import kanban.boardmanagement.entities.board.CardAdded;
import kanban.boardmanagement.entities.board.CardContentChanged;
import kanban.boardmanagement.entities.board.CardDescription;
import kanban.boardmanagement.entities.board.CardId;
import kanban.boardmanagement.entities.board.CardMoved;
import kanban.boardmanagement.entities.board.CardRemoved;
import kanban.boardmanagement.entities.board.CardRepositioned;
import kanban.boardmanagement.entities.board.CardTitle;
import kanban.boardmanagement.entities.board.ColumnAdded;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnMoved;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.entities.board.ColumnRemoved;
import kanban.boardmanagement.entities.board.ColumnRenamed;
import kanban.boardmanagement.entities.board.ColumnWipLimitRemoved;
import kanban.boardmanagement.entities.board.ColumnWipLimitSet;
import kanban.boardmanagement.entities.board.Position;
import kanban.boardmanagement.entities.board.WipLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** ES-7：事件帶格式版本；舊事件永遠讀得回來。 */
class BoardEventJsonTest {

    private static final Instant AT = Instant.parse("2026-11-14T01:00:00Z");
    private static final BoardId BOARD = new BoardId("board-1");
    private static final ColumnId TODO = new ColumnId("col-todo");
    private static final ColumnId DOING = new ColumnId("col-doing");
    private static final CardId CARD = new CardId("card-1");

    private final BoardEventJson json = new BoardEventJson();

    /** 每個「最新版本」樣本檔應該讀成的事件。 */
    private static final Map<String, BoardEvent> EXPECTED = Map.ofEntries(
            Map.entry("BoardCreated.v1", new BoardCreated(BOARD, new BoardName("產品開發"), AT)),
            Map.entry("BoardRenamed.v1", new BoardRenamed(BOARD, new BoardName("新產品"), AT)),
            Map.entry("BoardArchived.v1", new BoardArchived(BOARD, AT)),
            Map.entry("ColumnAdded.v1", new ColumnAdded(BOARD, TODO, new ColumnName("待辦"), AT)),
            Map.entry("ColumnRenamed.v1", new ColumnRenamed(BOARD, TODO, new ColumnName("準備中"), AT)),
            Map.entry("ColumnMoved.v1", new ColumnMoved(BOARD, TODO, new Position(2), AT)),
            Map.entry("ColumnWipLimitSet.v1", new ColumnWipLimitSet(BOARD, DOING, new WipLimit(3), AT)),
            Map.entry("ColumnWipLimitRemoved.v1", new ColumnWipLimitRemoved(BOARD, DOING, AT)),
            Map.entry("ColumnRemoved.v1", new ColumnRemoved(BOARD, new ColumnId("col-done"), AT)),
            Map.entry("CardAdded.v2",
                    new CardAdded(BOARD, CARD, TODO, new CardTitle("設計登入頁"), new CardDescription("含忘記密碼"), AT)),
            Map.entry("CardContentChanged.v1",
                    new CardContentChanged(BOARD, CARD, new CardTitle("設計登入頁（改）"), new CardDescription("補充說明"), AT)),
            Map.entry("CardMoved.v1", new CardMoved(BOARD, CARD, TODO, DOING, new Position(0), AT)),
            Map.entry("CardRepositioned.v1", new CardRepositioned(BOARD, CARD, TODO, new Position(1), AT)),
            Map.entry("CardRemoved.v1", new CardRemoved(BOARD, CARD, DOING, AT)));

    static Stream<BoardEvent> everyEvent() {
        return EXPECTED.values().stream();
    }

    private static Path samplesDirectory() {
        try {
            return Path.of(BoardEventJsonTest.class.getResource("/event-samples/README.md").toURI()).getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> sampleNames() throws IOException {
        try (Stream<Path> files = Files.list(samplesDirectory())) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted()
                    .toList();
        }
    }

    private static StoredEvent sample(String name) throws IOException {
        String[] parts = name.split("\\.v");
        String payload = Files.readString(samplesDirectory().resolve(name + ".json"));
        return new StoredEvent(parts[0], Integer.parseInt(parts[1]), payload);
    }

    @Test
    void 涵蓋全部14種事件() {
        assertThat(EXPECTED.values().stream().map(Object::getClass))
                .containsExactlyInAnyOrder(BoardEvent.class.getPermittedSubclasses());
    }

    @ParameterizedTest
    @MethodSource("everyEvent")
    void 每種事件_轉成JSON再轉回來_與原事件相等(BoardEvent event) {
        StoredEvent stored = json.toStored(event);

        assertThat(json.fromStored(stored)).isEqualTo(event);
        assertThat(stored.eventType()).isEqualTo(event.getClass().getSimpleName());
    }

    @Test
    void 寫出時一律用最新的格式版本() {
        assertThat(json.toStored(EXPECTED.get("CardAdded.v2")).schemaVersion()).isEqualTo(2);
        assertThat(json.toStored(EXPECTED.get("BoardCreated.v1")).schemaVersion()).isEqualTo(1);
    }

    @Test
    void 樣本檔都讀得回來() throws IOException {
        for (String name : sampleNames()) {
            BoardEvent event = json.fromStored(sample(name));

            assertThat(event.getClass().getSimpleName()).as(name).isEqualTo(name.split("\\.v")[0]);
            if (EXPECTED.containsKey(name)) {
                assertThat(event).as(name).isEqualTo(EXPECTED.get(name));
            }
        }
    }

    @Test
    void 落在現行驗證規則邊界上的舊事件都讀得回來() throws IOException, URISyntaxException {
        // ES-7：收緊值物件的驗證規則＝改變事件格式。這支測試在那時會失敗，提醒先處理已經存在的舊事件。
        Path limits = Path.of(BoardEventJsonTest.class.getResource("/event-samples-limits/README.md").toURI()).getParent();
        List<Path> files;
        try (Stream<Path> listed = Files.list(limits)) {
            files = listed.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList();
        }

        assertThat(files).hasSize(8);
        for (Path file : files) {
            String[] parts = file.getFileName().toString().replace(".json", "").split("\\.v");
            StoredEvent stored = new StoredEvent(parts[0], Integer.parseInt(parts[1]), Files.readString(file));

            BoardEvent event = json.fromStored(stored);

            assertThat(json.fromStored(json.toStored(event))).as(file.getFileName().toString()).isEqualTo(event);
        }
    }

    @Test
    void 每種事件的每個版本都有樣本檔() throws IOException {
        List<String> required = new ArrayList<>();
        json.latestVersions().forEach((type, latest) -> {
            for (int version = 1; version <= latest; version++) {
                required.add(type + ".v" + version);
            }
        });

        assertThat(sampleNames()).containsExactlyInAnyOrderElementsOf(required).hasSize(15);
    }

    @Test
    void CardAdded_v1_讀回來時說明是空字串() throws IOException {
        // v1 的 CardAdded 沒有 description 欄位；upcaster 補上空字串，資料庫裡的舊資料不動
        assertThat(json.fromStored(sample("CardAdded.v1")))
                .isEqualTo(new CardAdded(BOARD, CARD, TODO, new CardTitle("設計登入頁"), new CardDescription(""), AT));
    }

    @Test
    void 不認得的事件類型_丟UnknownEventTypeException() {
        assertThatThrownBy(() -> json.fromStored(new StoredEvent("BoardExploded", 1, "{}")))
                .isInstanceOf(UnknownEventTypeException.class)
                .hasMessageContaining("BoardExploded");
    }

    @Test
    void 比程式新的版本_丟UnknownEventTypeException() throws IOException {
        StoredEvent fromTheFuture = new StoredEvent("CardAdded", 3, sample("CardAdded.v2").payloadJson());

        assertThatThrownBy(() -> json.fromStored(fromTheFuture))
                .isInstanceOf(UnknownEventTypeException.class)
                .hasMessageContaining("CardAdded")
                .hasMessageContaining("3");
    }

    @Test
    void 版本號小於1_丟UnknownEventTypeException() {
        assertThatThrownBy(() -> json.fromStored(new StoredEvent("BoardArchived", 0, "{}")))
                .isInstanceOf(UnknownEventTypeException.class);
    }
}
