package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：卡片換到另一欄（看板上的事實），附在來源欄的停留時間（F7，由聚合算好）。 */
public record WorkItemMoved(BoardFlowId boardFlowId, ActivityPosition activityPosition, WorkItemId workItemId, FlowColumnId fromFlowColumnId, FlowColumnId toFlowColumnId, DwellTime dwellTimeInFromColumn, Instant occurredAt) implements BoardFlowEvent {
}
