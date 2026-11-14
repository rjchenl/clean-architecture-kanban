package kanban.fixtures.queryloadsaggregate.usecases.queries.viewthing.impl;

import kanban.fixtures.queryloadsaggregate.entities.Thing;

/** 違規樣本（CQRS-3）：查詢 Use Case 直接拿聚合來回答。 */
public final class ViewThingUseCase {
    Thing aggregate;
}
