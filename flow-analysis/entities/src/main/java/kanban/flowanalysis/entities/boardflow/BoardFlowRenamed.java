package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：看板改了名字（看板上的事實）。 */
public record BoardFlowRenamed(BoardFlowId boardFlowId, ActivityPosition activityPosition, FlowBoardName name, Instant occurredAt) implements BoardFlowEvent {
}
