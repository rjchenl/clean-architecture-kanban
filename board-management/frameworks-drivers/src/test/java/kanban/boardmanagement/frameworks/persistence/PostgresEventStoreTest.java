package kanban.boardmanagement.frameworks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kanban.boardmanagement.adapters.eventstore.BoardEventJson;
import kanban.boardmanagement.adapters.eventstore.EventStoreContract;
import kanban.boardmanagement.adapters.eventstore.PostgresEventStore;
import kanban.boardmanagement.usecases.commands.gateway.ConcurrentUpdateException;
import kanban.boardmanagement.usecases.commands.gateway.EventStore;
import kanban.boardmanagement.usecases.eventfeed.EventFeed;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** CA-6：PostgreSQL 版事件庫通過與記憶體版相同的契約測試。 */
class PostgresEventStoreTest extends EventStoreContract {

    private static PostgresEventStore store;

    @BeforeAll
    static void freshDatabase() {
        store = new PostgresEventStore(PostgresTestDatabase.freshDatabase(), new BoardEventJson());
    }

    @Override
    protected EventStore store() {
        return store;
    }

    @Override
    protected EventFeed feed() {
        return store;
    }

    @Test
    void 十條執行緒同時對同一條串流寫入同一個預期版本_恰好一條成功() throws Exception {
        String stream = newStreamId();
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger conflicted = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            String name = "t" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    store.append(stream, 0, List.of(created(name)));
                    succeeded.incrementAndGet();
                } catch (ConcurrentUpdateException e) {
                    conflicted.incrementAndGet();
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(unexpected).hasValue(0);
        assertThat(succeeded).hasValue(1);
        assertThat(conflicted).hasValue(threads - 1);
        assertThat(store.readStream(stream)).hasSize(1);
    }

    @Test
    void 存進資料庫的事件帶著類型名稱與格式版本() throws Exception {
        String stream = newStreamId();
        store.append(stream, 0, List.of(created("看板")));

        try (var connection = PostgresTestDatabase.dataSource().getConnection();
             var statement = connection.prepareStatement(
                     "SELECT event_type, schema_version, stream_version FROM board_events WHERE stream_id = ?")) {
            statement.setString(1, stream);
            try (var rows = statement.executeQuery()) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("event_type")).isEqualTo("BoardCreated");
                assertThat(rows.getInt("schema_version")).isEqualTo(1);
                assertThat(rows.getLong("stream_version")).isEqualTo(1);
            }
        }
    }
}
