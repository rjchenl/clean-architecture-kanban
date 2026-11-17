package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：卡片標題改了（看板上的事實）。 */
public record WorkItemTitleChanged(BoardFlowId boardFlowId, ActivityPosition activityPosition, WorkItemId workItemId, WorkItemTitle title, Instant occurredAt) implements BoardFlowEvent {
}
