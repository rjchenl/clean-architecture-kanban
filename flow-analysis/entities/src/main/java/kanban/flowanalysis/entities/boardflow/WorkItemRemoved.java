package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：卡片被刪掉了（看板上的事實）。被刪除不算離開所在欄，所以沒有停留時間（F7）。 */
public record WorkItemRemoved(BoardFlowId boardFlowId, ActivityPosition activityPosition, WorkItemId workItemId, FlowColumnId flowColumnId, Instant occurredAt) implements BoardFlowEvent {
}
