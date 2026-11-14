package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：欄位順序已調整。 */
public record ColumnMoved(BoardId boardId, ColumnId columnId, Position toPosition, Instant occurredAt) implements BoardEvent {
}
