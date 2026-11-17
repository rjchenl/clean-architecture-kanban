package kanban.flowanalysis.frameworks.persistence;

import kanban.flowanalysis.adapters.boardactivity.ActivityPositionStore;
import kanban.flowanalysis.adapters.boardactivity.ActivityPositionStoreContract;
import kanban.flowanalysis.adapters.boardactivity.PostgresActivityPositionStore;

/** 同一套契約測試對 PostgreSQL 版再跑一次（CA-6）。 */
class PostgresActivityPositionStoreTest extends ActivityPositionStoreContract {

    @Override
    protected ActivityPositionStore store() {
        return new PostgresActivityPositionStore(FlowAnalysisTestDatabase.freshDatabase());
    }
}
