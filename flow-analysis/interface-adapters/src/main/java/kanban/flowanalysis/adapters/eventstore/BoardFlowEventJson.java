package kanban.flowanalysis.adapters.eventstore;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 流程分析的領域事件 ⇄ JSON（ES-7）。和看板管理的 BoardEventJson 分開，各自演變。
 *
 * <p>事件類型名稱是寫死的字串，不從類別名稱反射取得：類別改名不可以讓舊事件讀不回來。
 * 寫出時一律用最新的格式版本；讀入舊版本時先經 upcaster 逐版轉成最新格式，資料庫裡的舊資料永遠不動。
 * 時間長度存成 ISO-8601 字串（例如 PT48H），來回轉換不會掉精度；可以沒有的值寫成 JSON null。
 */
public final class BoardFlowEventJson {

    /** 一種事件的格式：最新版本號、各舊版本升到下一版的轉換、最新格式的讀法。 */
    private record Format(int latestVersion,
                          Map<Integer, UnaryOperator<ObjectNode>> upcasters,
                          Function<ObjectNode, BoardFlowEvent> reader) {
    }

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<String, Format> formats = new LinkedHashMap<>();

    public BoardFlowEventJson() {
        register("BoardFlowCreated", n -> new BoardFlowCreated(flowId(n), position(n), boardName(n), at(n)));
        register("BoardFlowRenamed", n -> new BoardFlowRenamed(flowId(n), position(n), boardName(n), at(n)));
        register("BoardFlowArchived", n -> new BoardFlowArchived(flowId(n), position(n), at(n)));
        register("FlowColumnAdded", n -> new FlowColumnAdded(flowId(n), position(n), columnId(n, "flowColumnId"),
                new FlowColumnName(text(n, "name")), Stage.valueOf(text(n, "stage")), at(n)));
        register("FlowColumnRenamed", n -> new FlowColumnRenamed(flowId(n), position(n), columnId(n, "flowColumnId"),
                new FlowColumnName(text(n, "name")), at(n)));
        register("FlowColumnMoved", n -> new FlowColumnMoved(flowId(n), position(n), columnId(n, "flowColumnId"),
                new FlowColumnPosition(n.get("toPosition").asInt()), at(n)));
        register("FlowColumnRemoved", n -> new FlowColumnRemoved(flowId(n), position(n), columnId(n, "flowColumnId"),
                at(n)));
        register("WorkItemAdded", n -> new WorkItemAdded(flowId(n), position(n), workItemId(n),
                columnId(n, "flowColumnId"), new WorkItemTitle(text(n, "title")), at(n)));
        register("WorkItemTitleChanged", n -> new WorkItemTitleChanged(flowId(n), position(n), workItemId(n),
                new WorkItemTitle(text(n, "title")), at(n)));
        register("WorkItemMoved", n -> new WorkItemMoved(flowId(n), position(n), workItemId(n),
                columnId(n, "fromFlowColumnId"), columnId(n, "toFlowColumnId"),
                new DwellTime(Duration.parse(text(n, "dwellTimeInFromColumn"))), at(n)));
        register("WorkItemRemoved", n -> new WorkItemRemoved(flowId(n), position(n), workItemId(n),
                columnId(n, "flowColumnId"), at(n)));
        register("ColumnStageSet", n -> new ColumnStageSet(flowId(n), columnId(n, "flowColumnId"),
                Stage.valueOf(text(n, "stage")), at(n)));
        register("StuckThresholdSet", n -> new StuckThresholdSet(flowId(n),
                new StuckThreshold(n.get("thresholdDays").asInt()), at(n)));
        register("StuckThresholdRemoved", n -> new StuckThresholdRemoved(flowId(n), at(n)));
        register("WorkItemFlowResultChanged", n -> new WorkItemFlowResultChanged(flowId(n), workItemId(n),
                optionalInstant(n, "startedAt"), optionalInstant(n, "completedAt"), optionalLeadTime(n), at(n)));
        register("WorkItemStuckStatusChanged", n -> new WorkItemStuckStatusChanged(flowId(n), workItemId(n),
                n.get("stuck").asBoolean(), optionalColumnId(n), optionalInstant(n, "stuckSince"), at(n)));
    }

    /** 目前每一種事件都只有第 1 版；格式改變時把最新版本號加 1，並在 upcasters 補上舊版到新版的轉換。 */
    private void register(String eventType, Function<ObjectNode, BoardFlowEvent> reader) {
        formats.put(eventType, new Format(1, Map.of(), reader));
    }

    /** 每種事件目前最新的格式版本。 */
    public Map<String, Integer> latestVersions() {
        Map<String, Integer> versions = new LinkedHashMap<>();
        formats.forEach((type, format) -> versions.put(type, format.latestVersion()));
        return versions;
    }

    /** 事件 → 儲存格式。一律寫出最新版本。 */
    public StoredEvent toStored(BoardFlowEvent event) {
        ObjectNode n = mapper.createObjectNode();
        n.put("boardFlowId", event.boardFlowId().value());
        String eventType = switch (event) {
            case BoardFlowCreated e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("name", e.name().value());
                yield "BoardFlowCreated";
            }
            case BoardFlowRenamed e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("name", e.name().value());
                yield "BoardFlowRenamed";
            }
            case BoardFlowArchived e -> {
                n.put("activityPosition", e.activityPosition().value());
                yield "BoardFlowArchived";
            }
            case FlowColumnAdded e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("flowColumnId", e.flowColumnId().value());
                n.put("name", e.name().value());
                n.put("stage", e.stage().name());
                yield "FlowColumnAdded";
            }
            case FlowColumnRenamed e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("flowColumnId", e.flowColumnId().value());
                n.put("name", e.name().value());
                yield "FlowColumnRenamed";
            }
            case FlowColumnMoved e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("flowColumnId", e.flowColumnId().value());
                n.put("toPosition", e.toPosition().index());
                yield "FlowColumnMoved";
            }
            case FlowColumnRemoved e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("flowColumnId", e.flowColumnId().value());
                yield "FlowColumnRemoved";
            }
            case WorkItemAdded e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("workItemId", e.workItemId().value());
                n.put("flowColumnId", e.flowColumnId().value());
                n.put("title", e.title().value());
                yield "WorkItemAdded";
            }
            case WorkItemTitleChanged e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("workItemId", e.workItemId().value());
                n.put("title", e.title().value());
                yield "WorkItemTitleChanged";
            }
            case WorkItemMoved e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("workItemId", e.workItemId().value());
                n.put("fromFlowColumnId", e.fromFlowColumnId().value());
                n.put("toFlowColumnId", e.toFlowColumnId().value());
                n.put("dwellTimeInFromColumn", e.dwellTimeInFromColumn().value().toString());
                yield "WorkItemMoved";
            }
            case WorkItemRemoved e -> {
                n.put("activityPosition", e.activityPosition().value());
                n.put("workItemId", e.workItemId().value());
                n.put("flowColumnId", e.flowColumnId().value());
                yield "WorkItemRemoved";
            }
            case ColumnStageSet e -> {
                n.put("flowColumnId", e.flowColumnId().value());
                n.put("stage", e.stage().name());
                yield "ColumnStageSet";
            }
            case StuckThresholdSet e -> {
                n.put("thresholdDays", e.threshold().days());
                yield "StuckThresholdSet";
            }
            case StuckThresholdRemoved e -> "StuckThresholdRemoved";
            case WorkItemFlowResultChanged e -> {
                n.put("workItemId", e.workItemId().value());
                n.put("startedAt", e.startedAt() == null ? null : e.startedAt().toString());
                n.put("completedAt", e.completedAt() == null ? null : e.completedAt().toString());
                n.put("leadTime", e.leadTime() == null ? null : e.leadTime().value().toString());
                yield "WorkItemFlowResultChanged";
            }
            case WorkItemStuckStatusChanged e -> {
                n.put("workItemId", e.workItemId().value());
                n.put("stuck", e.stuck());
                n.put("flowColumnId", e.flowColumnId() == null ? null : e.flowColumnId().value());
                n.put("stuckSince", e.stuckSince() == null ? null : e.stuckSince().toString());
                yield "WorkItemStuckStatusChanged";
            }
        };
        n.put("occurredAt", event.occurredAt().toString());
        return new StoredEvent(eventType, formats.get(eventType).latestVersion(), mapper.writeValueAsString(n));
    }

    /** 儲存格式 → 事件。舊版本先逐版升級；不認得的類型或版本明確報錯。 */
    public BoardFlowEvent fromStored(StoredEvent stored) {
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

    private static String optionalText(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static BoardFlowId flowId(ObjectNode node) {
        return new BoardFlowId(text(node, "boardFlowId"));
    }

    private static ActivityPosition position(ObjectNode node) {
        return new ActivityPosition(node.get("activityPosition").asLong());
    }

    private static FlowBoardName boardName(ObjectNode node) {
        return new FlowBoardName(text(node, "name"));
    }

    private static FlowColumnId columnId(ObjectNode node, String field) {
        return new FlowColumnId(text(node, field));
    }

    private static FlowColumnId optionalColumnId(ObjectNode node) {
        String value = optionalText(node, "flowColumnId");
        return value == null ? null : new FlowColumnId(value);
    }

    private static WorkItemId workItemId(ObjectNode node) {
        return new WorkItemId(text(node, "workItemId"));
    }

    private static Instant optionalInstant(ObjectNode node, String field) {
        String value = optionalText(node, field);
        return value == null ? null : Instant.parse(value);
    }

    private static LeadTime optionalLeadTime(ObjectNode node) {
        String value = optionalText(node, "leadTime");
        return value == null ? null : new LeadTime(Duration.parse(value));
    }

    private static Instant at(ObjectNode node) {
        return Instant.parse(text(node, "occurredAt"));
    }
}
