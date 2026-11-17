package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：欄位換了左右位置（看板上的事實）。看板流程不靠欄位順序判斷，記下來給畫面用。 */
public record FlowColumnMoved(BoardFlowId boardFlowId, ActivityPosition activityPosition, FlowColumnId flowColumnId, FlowColumnPosition toPosition, Instant occurredAt) implements BoardFlowEvent {
}
