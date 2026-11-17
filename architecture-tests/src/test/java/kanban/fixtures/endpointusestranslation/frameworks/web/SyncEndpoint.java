package kanban.fixtures.endpointusestranslation.frameworks.web;

import kanban.fixtures.endpointusestranslation.adapters.boardactivity.SomeSync;

/** 違規樣本（CA-7、DDD-3）：HTTP 端點直接叫防腐層同步。 */
public final class SyncEndpoint {

    SomeSync sync;

    void sync() {
        sync.syncOnce();
    }
}
