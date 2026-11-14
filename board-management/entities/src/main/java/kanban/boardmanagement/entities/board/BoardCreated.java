package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 領域事件：看板已建立。 */
public record BoardCreated(BoardId boardId, BoardName name, Instant occurredAt) implements BoardEvent {
}
