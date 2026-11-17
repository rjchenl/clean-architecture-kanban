package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：看板上新增了一張卡片（看板上的事實）。 */
public record WorkItemAdded(BoardFlowId boardFlowId, ActivityPosition activityPosition, WorkItemId workItemId, FlowColumnId flowColumnId, WorkItemTitle title, Instant occurredAt) implements BoardFlowEvent {
}
