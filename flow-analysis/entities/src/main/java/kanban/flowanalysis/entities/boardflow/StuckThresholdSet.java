package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：使用者設定了卡住門檻。 */
public record StuckThresholdSet(BoardFlowId boardFlowId, StuckThreshold threshold, Instant occurredAt) implements BoardFlowEvent {
}
