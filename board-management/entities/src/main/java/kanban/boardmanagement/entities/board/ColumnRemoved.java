package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：欄位已刪除。 */
public record ColumnRemoved(BoardId boardId, ColumnId columnId, Instant occurredAt) implements BoardEvent {
}
