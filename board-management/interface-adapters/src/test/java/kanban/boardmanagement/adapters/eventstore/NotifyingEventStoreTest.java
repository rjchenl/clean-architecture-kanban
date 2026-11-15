package kanban.boardmanagement.adapters.eventstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.usecases.commands.gateway.ConcurrentUpdateException;
import org.junit.jupiter.api.Test;

class NotifyingEventStoreTest {

    private static final BoardEvent EVENT =
            new BoardCreated(new BoardId("b"), new BoardName("看板"), Instant.parse("2026-11-14T01:00:00Z"));

    private final InMemoryEventStore delegate = new InMemoryEventStore();
    private final AtomicInteger notifications = new AtomicInteger();

    @Test
    void append成功後呼叫通知_而且事件已經讀得到() {
        NotifyingEventStore store = new NotifyingEventStore(delegate, () -> {
            assertThat(delegate.readStream("s")).hasSize(1);
            notifications.incrementAndGet();
        });

        store.append("s", 0, List.of(EVENT));

        assertThat(notifications).hasValue(1);
    }

    @Test
    void 通知丟例外_append仍算成功() {
        NotifyingEventStore store = new NotifyingEventStore(delegate, () -> {
            throw new IllegalStateException("讀取模型更新失敗");
        });

        store.append("s", 0, List.of(EVENT));

        assertThat(delegate.readStream("s")).containsExactly(EVENT);
    }

    @Test
    void append失敗_不呼叫通知() {
        NotifyingEventStore store = new NotifyingEventStore(delegate, notifications::incrementAndGet);

        assertThatThrownBy(() -> store.append("s", 3, List.of(EVENT))).isInstanceOf(ConcurrentUpdateException.class);

        assertThat(notifications).hasValue(0);
    }

    @Test
    void readStream直接轉給底下的事件庫() {
        NotifyingEventStore store = new NotifyingEventStore(delegate, notifications::incrementAndGet);
        delegate.append("s", 0, List.of(EVENT));

        assertThat(store.readStream("s")).containsExactly(EVENT);
        assertThat(notifications).hasValue(0);
    }
}
