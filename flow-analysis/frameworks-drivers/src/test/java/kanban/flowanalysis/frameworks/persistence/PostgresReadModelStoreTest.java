package kanban.flowanalysis.frameworks.persistence;

import kanban.flowanalysis.adapters.projection.PostgresReadModelStore;
import kanban.flowanalysis.adapters.projection.ReadModelStore;
import kanban.flowanalysis.adapters.projection.ReadModelStoreContract;

/** 同一套流程分析讀取模型契約（含統計方式 S1、S2）對 PostgreSQL 版再跑一次（CA-6）。 */
class PostgresReadModelStoreTest extends ReadModelStoreContract {

    private final PostgresReadModelStore store =
            new PostgresReadModelStore(FlowAnalysisTestDatabase.freshDatabase(), CALENDAR);

    @Override
    protected ReadModelStore store() {
        return store;
    }
}
