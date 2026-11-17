package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：使用者取消了卡住門檻，之後不再判斷卡住（F8）。 */
public record StuckThresholdRemoved(BoardFlowId boardFlowId, Instant occurredAt) implements BoardFlowEvent {
}
