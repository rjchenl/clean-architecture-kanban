package kanban.flowanalysis.adapters.eventstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import kanban.flowanalysis.usecases.commands.gateway.ConcurrentUpdateException;
import kanban.flowanalysis.usecases.commands.gateway.EventStore;
import kanban.flowanalysis.usecases.eventfeed.EventFeed;
import org.junit.jupiter.api.Test;

class InMemoryEventStoreTest extends EventStoreContract {

    private final InMemoryEventStore store = new InMemoryEventStore();

    @Override
    protected EventStore store() {
        return store;
    }

    @Override
    protected EventFeed feed() {
        return store;
    }

    @Test
    void 十條執行緒同時寫入同一個預期版本_恰好一條成功() throws Exception {
        String stream = newStreamId();
        store.append(stream, 0, List.of(created("起點")));
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Future<?>> writers = java.util.stream.IntStream.range(0, 10).<Future<?>>mapToObj(i -> pool.submit(() -> {
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
