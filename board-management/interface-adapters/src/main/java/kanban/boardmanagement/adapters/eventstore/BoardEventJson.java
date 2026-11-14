package kanban.boardmanagement.adapters.eventstore;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
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
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 領域事件 ⇄ JSON（ES-7）。
 *
 * <p>事件類型名稱是寫死的字串，不從類別名稱反射取得：類別改名不可以讓舊事件讀不回來。
 * 寫出時一律用最新的格式版本；讀入舊版本時先經 upcaster 逐版轉成最新格式，資料庫裡的舊資料永遠不動。
 */
public final class BoardEventJson {

    /** 一種事件的格式：最新版本號、各舊版本升到下一版的轉換、最新格式的讀法。 */
    private record Format(int latestVersion,
                          Map<Integer, UnaryOperator<ObjectNode>> upcasters,
                          Function<ObjectNode, BoardEvent> reader) {
    }

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<String, Format> formats = new LinkedHashMap<>();

    public BoardEventJson() {
        register("BoardCreated", 1, Map.of(),
                n -> new BoardCreated(boardId(n), new BoardName(text(n, "name")), at(n)));
        register("BoardRenamed", 1, Map.of(),
                n -> new BoardRenamed(boardId(n), new BoardName(text(n, "name")), at(n)));
        register("BoardArchived", 1, Map.of(),
                n -> new BoardArchived(boardId(n), at(n)));
        register("ColumnAdded", 1, Map.of(),
                n -> new ColumnAdded(boardId(n), columnId(n, "columnId"), new ColumnName(text(n, "name")), at(n)));
        register("ColumnRenamed", 1, Map.of(),
                n -> new ColumnRenamed(boardId(n), columnId(n, "columnId"), new ColumnName(text(n, "name")), at(n)));
        register("ColumnMoved", 1, Map.of(),
                n -> new ColumnMoved(boardId(n), columnId(n, "columnId"), position(n), at(n)));
        register("ColumnWipLimitSet", 1, Map.of(),
                n -> new ColumnWipLimitSet(boardId(n), columnId(n, "columnId"),
                        new WipLimit(n.get("wipLimit").asInt()), at(n)));
        register("ColumnWipLimitRemoved", 1, Map.of(),
                n -> new ColumnWipLimitRemoved(boardId(n), columnId(n, "columnId"), at(n)));
        register("ColumnRemoved", 1, Map.of(),
                n -> new ColumnRemoved(boardId(n), columnId(n, "columnId"), at(n)));
        // CardAdded v1 沒有 description；v2 加上。v1 → v2：補空字串。
        register("CardAdded", 2, Map.of(1, n -> n.put("description", "")),
                n -> new CardAdded(boardId(n), cardId(n), columnId(n, "columnId"),
                        new CardTitle(text(n, "title")), new CardDescription(text(n, "description")), at(n)));
        register("CardContentChanged", 1, Map.of(),
                n -> new CardContentChanged(boardId(n), cardId(n),
                        new CardTitle(text(n, "title")), new CardDescription(text(n, "description")), at(n)));
        register("CardMoved", 1, Map.of(),
                n -> new CardMoved(boardId(n), cardId(n), columnId(n, "fromColumnId"), columnId(n, "toColumnId"),
                        position(n), at(n)));
        register("CardRepositioned", 1, Map.of(),
                n -> new CardRepositioned(boardId(n), cardId(n), columnId(n, "columnId"), position(n), at(n)));
        register("CardRemoved", 1, Map.of(),
                n -> new CardRemoved(boardId(n), cardId(n), columnId(n, "columnId"), at(n)));
    }

    private void register(String eventType, int latestVersion, Map<Integer, UnaryOperator<ObjectNode>> upcasters,
                          Function<ObjectNode, BoardEvent> reader) {
        formats.put(eventType, new Format(latestVersion, upcasters, reader));
    }

    /** 每種事件目前最新的格式版本。 */
    public Map<String, Integer> latestVersions() {
        Map<String, Integer> versions = new LinkedHashMap<>();
        formats.forEach((type, format) -> versions.put(type, format.latestVersion()));
        return versions;
    }

    /** 事件 → 儲存格式。一律寫出最新版本。 */
    public StoredEvent toStored(BoardEvent event) {
        ObjectNode n = mapper.createObjectNode();
        n.put("boardId", event.boardId().value());
        String eventType = switch (event) {
            case BoardCreated e -> {
                n.put("name", e.name().value());
                yield "BoardCreated";
            }
            case BoardRenamed e -> {
                n.put("name", e.name().value());
                yield "BoardRenamed";
            }
            case BoardArchived e -> "BoardArchived";
            case ColumnAdded e -> {
                n.put("columnId", e.columnId().value());
                n.put("name", e.name().value());
                yield "ColumnAdded";
            }
            case ColumnRenamed e -> {
                n.put("columnId", e.columnId().value());
                n.put("name", e.name().value());
                yield "ColumnRenamed";
            }
            case ColumnMoved e -> {
                n.put("columnId", e.columnId().value());
                n.put("toPosition", e.toPosition().index());
                yield "ColumnMoved";
            }
            case ColumnWipLimitSet e -> {
                n.put("columnId", e.columnId().value());
                n.put("wipLimit", e.wipLimit().value());
                yield "ColumnWipLimitSet";
            }
            case ColumnWipLimitRemoved e -> {
                n.put("columnId", e.columnId().value());
                yield "ColumnWipLimitRemoved";
            }
            case ColumnRemoved e -> {
                n.put("columnId", e.columnId().value());
                yield "ColumnRemoved";
            }
            case CardAdded e -> {
                n.put("cardId", e.cardId().value());
                n.put("columnId", e.columnId().value());
                n.put("title", e.title().value());
                n.put("description", e.description().value());
                yield "CardAdded";
            }
            case CardContentChanged e -> {
                n.put("cardId", e.cardId().value());
                n.put("title", e.title().value());
                n.put("description", e.description().value());
                yield "CardContentChanged";
            }
            case CardMoved e -> {
                n.put("cardId", e.cardId().value());
                n.put("fromColumnId", e.fromColumnId().value());
                n.put("toColumnId", e.toColumnId().value());
                n.put("toPosition", e.toPosition().index());
                yield "CardMoved";
            }
            case CardRepositioned e -> {
                n.put("cardId", e.cardId().value());
                n.put("columnId", e.columnId().value());
                n.put("toPosition", e.toPosition().index());
                yield "CardRepositioned";
            }
            case CardRemoved e -> {
                n.put("cardId", e.cardId().value());
                n.put("columnId", e.columnId().value());
                yield "CardRemoved";
            }
        };
        n.put("occurredAt", event.occurredAt().toString());
        return new StoredEvent(eventType, formats.get(eventType).latestVersion(), mapper.writeValueAsString(n));
    }

    /** 儲存格式 → 事件。舊版本先逐版升級；不認得的類型或版本明確報錯。 */
    public BoardEvent fromStored(StoredEvent stored) {
        Format format = formats.get(stored.eventType());
        if (format == null || stored.schemaVersion() < 1 || stored.schemaVersion() > format.latestVersion()) {
            throw new UnknownEventTypeException(stored.eventType(), stored.schemaVersion());
        }
        ObjectNode node = (ObjectNode) mapper.readTree(stored.payloadJson());
        for (int version = stored.schemaVersion(); version < format.latestVersion(); version++) {
            node = format.upcasters().get(version).apply(node);
        }
        return format.reader().apply(node);
    }

    private static String text(ObjectNode node, String field) {
        return node.get(field).asString();
    }

    private static BoardId boardId(ObjectNode node) {
        return new BoardId(text(node, "boardId"));
    }

    private static ColumnId columnId(ObjectNode node, String field) {
        return new ColumnId(text(node, field));
    }

    private static CardId cardId(ObjectNode node) {
        return new CardId(text(node, "cardId"));
    }

    private static Position position(ObjectNode node) {
        return new Position(node.get("toPosition").asInt());
    }

    private static Instant at(ObjectNode node) {
        return Instant.parse(text(node, "occurredAt"));
    }
}
