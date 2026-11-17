package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：看板封存了（看板上的事實）；流程數字照常顯示、設定照常可以改（F13）。 */
public record BoardFlowArchived(BoardFlowId boardFlowId, ActivityPosition activityPosition, Instant occurredAt) implements BoardFlowEvent {
}
