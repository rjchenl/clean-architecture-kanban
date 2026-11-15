package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：欄位的在製品上限已取消。 */
public record ColumnWipLimitRemoved(BoardId boardId, ColumnId columnId, Instant occurredAt) implements BoardEvent {
}
