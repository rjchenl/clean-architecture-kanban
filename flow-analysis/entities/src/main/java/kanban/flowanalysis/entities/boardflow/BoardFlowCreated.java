package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：看板流程建立：流程分析開始記錄一個看板（看板上的事實）。 */
public record BoardFlowCreated(BoardFlowId boardFlowId, ActivityPosition activityPosition, FlowBoardName name, Instant occurredAt) implements BoardFlowEvent {
}
