package kanban.fixtures.aggregates.entities.board;

import kanban.fixtures.aggregates.entities.sprint.SprintLike;

/** 違規樣本（DDD-5）：看板聚合直接持有另一個聚合的物件。 */
public final class BoardLike {
    SprintLike sprint;
}
