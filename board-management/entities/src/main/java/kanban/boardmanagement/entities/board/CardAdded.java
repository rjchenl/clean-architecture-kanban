package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：卡片已新增（加在該欄最下面）。 */
public record CardAdded(BoardId boardId, CardId cardId, ColumnId columnId, CardTitle title, CardDescription description, Instant occurredAt) implements BoardEvent {
}
