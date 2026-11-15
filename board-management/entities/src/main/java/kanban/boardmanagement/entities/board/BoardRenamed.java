package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：看板已改名。 */
public record BoardRenamed(BoardId boardId, BoardName name, Instant occurredAt) implements BoardEvent {
}
