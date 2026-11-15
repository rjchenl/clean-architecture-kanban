package kanban.fixtures.commandusesquery.usecases.commands.dothing;

import kanban.fixtures.commandusesquery.usecases.queries.viewthing.ThingSnapshot;

/** 違規樣本（CQRS-1）：指令端與查詢端共用同一個模型。 */
public record DoThingRequest(ThingSnapshot snapshot) {
}
