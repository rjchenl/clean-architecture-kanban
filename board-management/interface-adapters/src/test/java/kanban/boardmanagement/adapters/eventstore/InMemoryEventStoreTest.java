package kanban.boardmanagement.adapters.eventstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kanban.boardmanagement.usecases.commands.gateway.ConcurrentUpdateException;
import kanban.boardmanagement.usecases.commands.gateway.EventStore;
import kanban.boardmanagement.usecases.eventfeed.EventFeed;
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
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger conflicted = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            String name = "t" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    store.append(stream, 0, List.of(created(name)));
                    succeeded.incrementAndGet();
                } catch (ConcurrentUpdateException e) {
                    conflicted.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(succeeded).hasValue(1);
        assertThat(conflicted).hasValue(threads - 1);
        assertThat(store.readStream(stream)).hasSize(1);
    }
}
