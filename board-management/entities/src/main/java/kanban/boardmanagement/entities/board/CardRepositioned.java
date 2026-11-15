package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：卡片已在同一欄內調整順序。 */
public record CardRepositioned(BoardId boardId, CardId cardId, ColumnId columnId, Position toPosition, Instant occurredAt) implements BoardEvent {
}
