package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：卡片已刪除。 */
public record CardRemoved(BoardId boardId, CardId cardId, ColumnId columnId, Instant occurredAt) implements BoardEvent {
}
