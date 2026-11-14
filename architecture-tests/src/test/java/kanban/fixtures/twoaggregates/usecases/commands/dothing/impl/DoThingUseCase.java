package kanban.fixtures.twoaggregates.usecases.commands.dothing.impl;

import kanban.fixtures.twoaggregates.usecases.commands.gateway.BoardRepository;
import kanban.fixtures.twoaggregates.usecases.commands.gateway.SprintRepository;

/** 違規樣本（DDD-5）：一個指令同時修改兩個聚合。 */
public final class DoThingUseCase {
    BoardRepository boards;
    SprintRepository sprints;
}
