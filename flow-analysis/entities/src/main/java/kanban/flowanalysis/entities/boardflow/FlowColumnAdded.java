package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：看板加了一欄（看板上的事實）。階段一律從未分類開始，由聚合寫進事件（F2）。 */
public record FlowColumnAdded(BoardFlowId boardFlowId, ActivityPosition activityPosition, FlowColumnId flowColumnId, FlowColumnName name, Stage stage, Instant occurredAt) implements BoardFlowEvent {
}
