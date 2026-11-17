package kanban.flowanalysis.adapters.eventstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import kanban.flowanalysis.usecases.commands.gateway.ConcurrentUpdateException;
import org.junit.jupiter.api.Test;

/** 事件寫入成功後通知一聲；通知失敗不影響寫入的結果（設計第 7.6 節）。 */
class NotifyingEventStoreTest {

    private final InMemoryEventStore delegate = new InMemoryEventStore();
    private final AtomicInteger notified = new AtomicInteger();

    @Test
    void 寫入成功後通知() {
        NotifyingEventStore store = new NotifyingEventStore(delegate, notified::incrementAndGet);

        store.append("board-flow-1", 0, List.of(EventStoreContract.created("一")));

        assertThat(notified.get()).isEqualTo(1);
        assertThat(store.readStream("board-flow-1")).hasSize(1);
    }

    @Test
    void 通知失敗_寫入照樣成功_例外不往外丟() {
        NotifyingEventStore store = new NotifyingEventStore(delegate, () -> {
            throw new IllegalStateException("讀取模型故障");
        });

        assertThatCode(() -> store.append("board-flow-1", 0, List.of(EventStoreContract.created("一"))))
                .doesNotThrowAnyException();
        assertThat(delegate.readStream("board-flow-1")).hasSize(1);
    }

    @Test
    void 寫入失敗_不通知_例外照常往外丟() {
        NotifyingEventStore store = new NotifyingEventStore(delegate, notified::incrementAndGet);

        assertThatThrownBy(() -> store.append("board-flow-1", 3, List.of(EventStoreContract.created("一"))))
                .isInstanceOf(ConcurrentUpdateException.class);
        assertThat(notified.get()).isZero();
    }
}
