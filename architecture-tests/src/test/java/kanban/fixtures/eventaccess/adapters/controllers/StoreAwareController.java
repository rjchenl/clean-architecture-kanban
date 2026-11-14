package kanban.fixtures.eventaccess.adapters.controllers;

import kanban.fixtures.eventaccess.usecases.commands.gateway.EventStore;

/** 違規樣本（CA-11 ISP）：Controller 直接使用 EventStore。 */
public final class StoreAwareController {
    EventStore store;
}
