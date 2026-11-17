package kanban.flowanalysis.adapters.eventstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import kanban.flowanalysis.entities.boardflow.ActivityPosition;
import kanban.flowanalysis.entities.boardflow.BoardFlowArchived;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.BoardFlowRenamed;
import kanban.flowanalysis.entities.boardflow.ColumnStageSet;
import kanban.flowanalysis.entities.boardflow.DwellTime;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import kanban.flowanalysis.entities.boardflow.FlowColumnAdded;
import kanban.flowanalysis.entities.boardflow.FlowColumnId;
import kanban.flowanalysis.entities.boardflow.FlowColumnMoved;
import kanban.flowanalysis.entities.boardflow.FlowColumnName;
import kanban.flowanalysis.entities.boardflow.FlowColumnPosition;
import kanban.flowanalysis.entities.boardflow.FlowColumnRemoved;
import kanban.flowanalysis.entities.boardflow.FlowColumnRenamed;
import kanban.flowanalysis.entities.boardflow.LeadTime;
import kanban.flowanalysis.entities.boardflow.Stage;
import kanban.flowanalysis.entities.boardflow.StuckThreshold;
import kanban.flowanalysis.entities.boardflow.StuckThresholdRemoved;
import kanban.flowanalysis.entities.boardflow.StuckThresholdSet;
import kanban.flowanalysis.entities.boardflow.WorkItemAdded;
import kanban.flowanalysis.entities.boardflow.WorkItemFlowResultChanged;
import kanban.flowanalysis.entities.boardflow.WorkItemId;
import kanban.flowanalysis.entities.boardflow.WorkItemMoved;
import kanban.flowanalysis.entities.boardflow.WorkItemRemoved;
import kanban.flowanalysis.entities.boardflow.WorkItemStuckStatusChanged;
import kanban.flowanalysis.entities.boardflow.WorkItemTitle;
import kanban.flowanalysis.entities.boardflow.WorkItemTitleChanged;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** ES-7：流程分析的事件帶格式版本；舊事件永遠讀得回來。 */
class BoardFlowEventJsonTest {

    private static final Instant AT = Instant.parse("2026-11-16T01:00:00Z");
    private static final BoardFlowId FLOW = new BoardFlowId("board-1");
    private static final FlowColumnId TODO = new FlowColumnId("col-todo");
    private static final FlowColumnId DOING = new FlowColumnId("col-doing");
    private static final WorkItemId CARD = new WorkItemId("card-1");
    private static final Duration TWO_DAYS = Duration.ofHours(48);

    private final BoardFlowEventJson json = new BoardFlowEventJson();

    private static ActivityPosition at(long position) {
        return new ActivityPosition(position);
    }

    /** 每個樣本檔應該讀成的事件。 */
    private static final Map<String, BoardFlowEvent> EXPECTED = Map.ofEntries(
            Map.entry("BoardFlowCreated.v1", new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), AT)),
            Map.entry("BoardFlowRenamed.v1", new BoardFlowRenamed(FLOW, at(2), new FlowBoardName("新產品"), AT)),
            Map.entry("BoardFlowArchived.v1", new BoardFlowArchived(FLOW, at(9), AT)),
            Map.entry("FlowColumnAdded.v1",
                    new FlowColumnAdded(FLOW, at(3), TODO, new FlowColumnName("待辦"), Stage.UNCLASSIFIED, AT)),
            Map.entry("FlowColumnRenamed.v1", new FlowColumnRenamed(FLOW, at(4), TODO, new FlowColumnName("準備中"), AT)),
            Map.entry("FlowColumnMoved.v1", new FlowColumnMoved(FLOW, at(5), TODO, new FlowColumnPosition(2), AT)),
            Map.entry("FlowColumnRemoved.v1", new FlowColumnRemoved(FLOW, at(6), new FlowColumnId("col-done"), AT)),
            Map.entry("WorkItemAdded.v1", new WorkItemAdded(FLOW, at(7), CARD, TODO, new WorkItemTitle("設計登入頁"), AT)),
            Map.entry("WorkItemTitleChanged.v1",
                    new WorkItemTitleChanged(FLOW, at(8), CARD, new WorkItemTitle("設計登入頁（改）"), AT)),
            Map.entry("WorkItemMoved.v1", new WorkItemMoved(FLOW, at(10), CARD, TODO, DOING, new DwellTime(TWO_DAYS), AT)),
            Map.entry("WorkItemRemoved.v1", new WorkItemRemoved(FLOW, at(11), CARD, DOING, AT)),
            Map.entry("ColumnStageSet.v1", new ColumnStageSet(FLOW, DOING, Stage.IN_PROGRESS, AT)),
            Map.entry("StuckThresholdSet.v1", new StuckThresholdSet(FLOW, new StuckThreshold(5), AT)),
            Map.entry("StuckThresholdRemoved.v1", new StuckThresholdRemoved(FLOW, AT)),
            Map.entry("WorkItemFlowResultChanged.v1", new WorkItemFlowResultChanged(FLOW, CARD,
                    Instant.parse("2026-11-14T01:00:00Z"), Instant.parse("2026-11-16T01:00:00Z"), new LeadTime(TWO_DAYS), AT)),
            Map.entry("WorkItemStuckStatusChanged.v1", new WorkItemStuckStatusChanged(FLOW, CARD, true, DOING,
                    Instant.parse("2026-11-10T01:00:00Z"), AT)));

    /** 樣本之外，再加上「可以沒有的值」是 null 的寫法，以及帶奈秒的時間長度。 */
    static Stream<BoardFlowEvent> everyEvent() {
        return Stream.concat(EXPECTED.values().stream(), Stream.of(
                new WorkItemFlowResultChanged(FLOW, CARD, Instant.parse("2026-11-14T01:00:00Z"), null, null, AT),
                new WorkItemFlowResultChanged(FLOW, CARD, null, null, null, AT),
                new WorkItemStuckStatusChanged(FLOW, CARD, false, null, null, AT),
                new WorkItemMoved(FLOW, at(12), CARD, TODO, DOING, new DwellTime(Duration.ofSeconds(90, 123_456_789)),
                        AT.plusNanos(5))));
    }

    private static Path samplesDirectory() {
        try {
            return Path.of(BoardFlowEventJsonTest.class.getResource("/event-samples/README.md").toURI()).getParent();
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
    void 涵蓋全部16種事件() {
        assertThat(EXPECTED.values().stream().map(Object::getClass))
                .containsExactlyInAnyOrder(BoardFlowEvent.class.getPermittedSubclasses());
    }

    @ParameterizedTest
    @MethodSource("everyEvent")
    void 每種事件_轉成JSON再轉回來_與原事件相等(BoardFlowEvent event) {
        StoredEvent stored = json.toStored(event);

        assertThat(json.fromStored(stored)).isEqualTo(event);
        assertThat(stored.eventType()).isEqualTo(event.getClass().getSimpleName());
        assertThat(stored.schemaVersion()).isEqualTo(1);
    }

    @Test
    void 樣本檔都讀得回來() throws IOException {
        for (String name : sampleNames()) {
            assertThat(json.fromStored(sample(name))).as(name).isEqualTo(EXPECTED.get(name));
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

        assertThat(sampleNames()).containsExactlyInAnyOrderElementsOf(required).hasSize(16);
    }

    @Test
    void 不認得的事件類型_丟UnknownEventTypeException() {
        assertThatThrownBy(() -> json.fromStored(new StoredEvent("BoardFlowExploded", 1, "{}")))
                .isInstanceOf(UnknownEventTypeException.class)
                .hasMessageContaining("BoardFlowExploded");
    }

    @Test
    void 比程式新的版本_丟UnknownEventTypeException() throws IOException {
        StoredEvent fromTheFuture = new StoredEvent("WorkItemMoved", 2, sample("WorkItemMoved.v1").payloadJson());

        assertThatThrownBy(() -> json.fromStored(fromTheFuture))
                .isInstanceOf(UnknownEventTypeException.class)
                .hasMessageContaining("WorkItemMoved")
                .hasMessageContaining("2");
    }

    @Test
    void 版本號小於1_丟UnknownEventTypeException() {
        assertThatThrownBy(() -> json.fromStored(new StoredEvent("StuckThresholdRemoved", 0, "{}")))
                .isInstanceOf(UnknownEventTypeException.class);
    }
}
