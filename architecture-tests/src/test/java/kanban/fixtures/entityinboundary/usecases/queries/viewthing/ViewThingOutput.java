package kanban.fixtures.entityinboundary.usecases.queries.viewthing;

import kanban.fixtures.entityinboundary.entities.Thing;

/** 違規樣本（CA-5）：Output Boundary 直接把領域物件交給 Presenter。 */
public interface ViewThingOutput {
    void present(Thing thing);
}
