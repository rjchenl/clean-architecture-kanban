package kanban.fixtures.readmodeloutside.adapters.eventstore;

import kanban.fixtures.readmodeloutside.adapters.system.MetricsCache;

/** 違規樣本（CQRS-5）：事件寫入後直接改讀取模型，不經 Projection。 */
public final class WriteThroughEventStore {

    private final MetricsCache cache;

    public WriteThroughEventStore(MetricsCache cache) {
        this.cache = cache;
    }

    public void append(String id) {
        cache.put(id, "updated");
    }
}
