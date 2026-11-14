package kanban.fixtures.readmodelimpl.adapters.system;

import kanban.fixtures.readmodelimpl.adapters.projection.InMemoryReadModelStore;

/** 違規樣本（CQRS-5）：不透過 ReadModelStore 介面，直接拿具體實作來改讀取模型。 */
public final class Meddler {
    private final InMemoryReadModelStore store;

    public Meddler(InMemoryReadModelStore store) {
        this.store = store;
    }

    public void meddle() {
        store.saveBoard("tampered");
    }
}
