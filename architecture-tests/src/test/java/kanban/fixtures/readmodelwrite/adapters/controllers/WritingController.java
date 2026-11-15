package kanban.fixtures.readmodelwrite.adapters.controllers;

import kanban.fixtures.readmodelwrite.adapters.projection.ReadModelStore;

/** 違規樣本（CQRS-5）：Controller 繞過事件，直接改讀取模型。 */
public final class WritingController {
    private final ReadModelStore store;

    public WritingController(ReadModelStore store) {
        this.store = store;
    }

    public void handle() {
        store.saveBoard("board");
    }
}
