package kanban.boardmanagement.frameworks.persistence;

import kanban.boardmanagement.adapters.projection.BoardActivityLogStore;
import kanban.boardmanagement.adapters.projection.BoardActivityLogStoreContract;
import kanban.boardmanagement.adapters.projection.PostgresBoardActivityLog;

/** 同一套看板動態紀錄的契約測試對 PostgreSQL 版再跑一次（CA-6）。 */
class PostgresBoardActivityLogTest extends BoardActivityLogStoreContract {

    private final PostgresBoardActivityLog store = new PostgresBoardActivityLog(PostgresTestDatabase.freshDatabase());

    @Override
    protected BoardActivityLogStore store() {
        return store;
    }
}
