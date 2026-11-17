package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：欄位改了名字（看板上的事實）。 */
public record FlowColumnRenamed(BoardFlowId boardFlowId, ActivityPosition activityPosition, FlowColumnId flowColumnId, FlowColumnName name, Instant occurredAt) implements BoardFlowEvent {
}
