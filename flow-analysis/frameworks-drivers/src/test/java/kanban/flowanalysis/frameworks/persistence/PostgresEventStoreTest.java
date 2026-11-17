package kanban.flowanalysis.frameworks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import kanban.flowanalysis.adapters.eventstore.BoardFlowEventJson;
import kanban.flowanalysis.adapters.eventstore.EventStoreContract;
import kanban.flowanalysis.adapters.eventstore.PostgresEventStore;
import kanban.flowanalysis.usecases.commands.gateway.ConcurrentUpdateException;
import kanban.flowanalysis.usecases.commands.gateway.EventStore;
import kanban.flowanalysis.usecases.eventfeed.EventFeed;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 同一套流程分析 Event Store 契約對 PostgreSQL 版再跑一次（CA-6、ES-2、ES-6）。 */
class PostgresEventStoreTest extends EventStoreContract {

    private static DataSource dataSource;
    private static PostgresEventStore store;

    @BeforeAll
    static void migrate() {
        dataSource = FlowAnalysisTestDatabase.freshDatabase();
        store = new PostgresEventStore(dataSource, new BoardFlowEventJson());
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
        store.append(stream, 0, List.of(created("起點")));
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Future<?>> writers = IntStream.range(0, 10).<Future<?>>mapToObj(i -> pool.submit(() -> {
                start.await();
                try {
                    store.append(stream, 1, List.of(renamed("寫入者" + i)));
                    succeeded.incrementAndGet();
                } catch (ConcurrentUpdateException expected) {
                    // 版本衝突：別人先寫了
                }
                return null;
            })).toList();
            start.countDown();
            for (Future<?> writer : writers) {
                writer.get();
            }
        } finally {
            pool.shutdown();
        }

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(store.readStream(stream)).hasSize(2);
    }
}
