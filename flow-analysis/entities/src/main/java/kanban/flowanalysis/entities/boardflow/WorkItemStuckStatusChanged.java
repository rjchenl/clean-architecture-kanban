package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：工作項目變成卡住，或不再卡住（F9、F10）。不再卡住時 flowColumnId 與 stuckSince 是 null。 */
public record WorkItemStuckStatusChanged(BoardFlowId boardFlowId, WorkItemId workItemId, boolean stuck, FlowColumnId flowColumnId, Instant stuckSince, Instant occurredAt) implements BoardFlowEvent {
}
