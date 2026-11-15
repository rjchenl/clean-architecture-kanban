package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：欄位已改名。 */
public record ColumnRenamed(BoardId boardId, ColumnId columnId, ColumnName name, Instant occurredAt) implements BoardEvent {
}
