package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：看板已封存。 */
public record BoardArchived(BoardId boardId, Instant occurredAt) implements BoardEvent {
}
