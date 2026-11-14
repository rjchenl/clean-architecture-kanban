package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：欄位的在製品上限已設定。 */
public record ColumnWipLimitSet(BoardId boardId, ColumnId columnId, WipLimit wipLimit, Instant occurredAt) implements BoardEvent {
}
