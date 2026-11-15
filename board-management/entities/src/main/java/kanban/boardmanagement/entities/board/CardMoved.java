package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：卡片已換到另一欄。 */
public record CardMoved(BoardId boardId, CardId cardId, ColumnId fromColumnId, ColumnId toColumnId, Position toPosition, Instant occurredAt) implements BoardEvent {
}
