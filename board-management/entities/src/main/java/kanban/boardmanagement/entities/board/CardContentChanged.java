package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：卡片內容已修改。 */
public record CardContentChanged(BoardId boardId, CardId cardId, CardTitle title, CardDescription description, Instant occurredAt) implements BoardEvent {
}
