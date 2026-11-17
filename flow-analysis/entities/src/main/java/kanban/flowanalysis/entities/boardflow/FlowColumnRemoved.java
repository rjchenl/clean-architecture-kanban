package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：欄位被刪掉了（看板上的事實）；階段仍保留，用來判斷曾經過那一欄的歷史（F12）。 */
public record FlowColumnRemoved(BoardFlowId boardFlowId, ActivityPosition activityPosition, FlowColumnId flowColumnId, Instant occurredAt) implements BoardFlowEvent {
}
