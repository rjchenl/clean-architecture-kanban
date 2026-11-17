package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：使用者替一欄指定了階段。 */
public record ColumnStageSet(BoardFlowId boardFlowId, FlowColumnId flowColumnId, Stage stage, Instant occurredAt) implements BoardFlowEvent {
}
