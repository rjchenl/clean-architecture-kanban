package kanban.boardmanagement.frameworks.persistence;

import kanban.boardmanagement.adapters.projection.PostgresReadModelStore;
import kanban.boardmanagement.adapters.projection.ReadModelStore;
import kanban.boardmanagement.adapters.projection.ReadModelStoreContract;
import org.junit.jupiter.api.BeforeAll;

/** CA-6：PostgreSQL 版讀取模型通過與記憶體版相同的契約測試。 */
class PostgresReadModelStoreTest extends ReadModelStoreContract {

    private static PostgresReadModelStore store;

    @BeforeAll
    static void freshDatabase() {
        store = new PostgresReadModelStore(PostgresTestDatabase.freshDatabase());
    }

    @Override
    protected ReadModelStore store() {
        return store;
    }
}
