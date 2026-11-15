package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：欄位已新增（加在最右邊）。 */
public record ColumnAdded(BoardId boardId, ColumnId columnId, ColumnName name, Instant occurredAt) implements BoardEvent {
}
