package kanban.flowanalysis.adapters.eventstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import kanban.flowanalysis.entities.boardflow.ActivityPosition;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.BoardFlowRenamed;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import kanban.flowanalysis.entities.boardflow.StuckThreshold;
import kanban.flowanalysis.entities.boardflow.StuckThresholdSet;
import kanban.flowanalysis.usecases.commands.gateway.ConcurrentUpdateException;
import kanban.flowanalysis.usecases.commands.gateway.EventStore;
import kanban.flowanalysis.usecases.eventfeed.EventFeed;
import kanban.flowanalysis.usecases.eventfeed.RecordedEvent;
import org.junit.jupiter.api.Test;

/**
 * 流程分析 Event Store 的契約（CA-6、ES-2、ES-6）：記憶體版與 PostgreSQL 版都要通過同一套測試。
 * 和看板管理的契約分開：兩個情境的介面只是長得像，會各自演變（DDD-2）。
 *
 * <p>測試不假設事件庫是空的（PostgreSQL 的事件表不能清）：串流識別碼用隨機值，
 * EventFeed 的測試先取得目前最後的位置當基準。
 */
public abstract class EventStoreContract {

    protected static final Instant AT = Instant.parse("2026-11-16T01:00:00Z");

    protected abstract EventStore store();

    protected abstract EventFeed feed();

    protected static String newStreamId() {
        return "board-flow-" + UUID.randomUUID();
    }

    protected static BoardFlowEvent created(String name) {
        return new BoardFlowCreated(new BoardFlowId("f-" + name), new ActivityPosition(1), new FlowBoardName(name), AT);
    }

    protected static BoardFlowEvent renamed(String name) {
        return new BoardFlowRenamed(new BoardFlowId("f-" + name), new ActivityPosition(2), new FlowBoardName(name), AT);
    }

    protected static BoardFlowEvent thresholdSet(String name) {
        return new StuckThresholdSet(new BoardFlowId("f-" + name), new StuckThreshold(3), AT);
    }

    private long lastPosition() {
        long position = 0;
        while (true) {
            List<RecordedEvent> batch = feed().readAfter(position, 500);
            if (batch.isEmpty()) {
                return position;
            }
            position = batch.get(batch.size() - 1).position();
        }
    }

    @Test
    void 新增後讀得回來_順序不變() {
        String stream = newStreamId();

        store().append(stream, 0, List.of(created("一"), renamed("二")));
        store().append(stream, 2, List.of(thresholdSet("三")));

        assertThat(store().readStream(stream)).containsExactly(created("一"), renamed("二"), thresholdSet("三"));
    }

    @Test
    void 不存在的串流_回空清單() {
        assertThat(store().readStream(newStreamId())).isEmpty();
    }

    @Test
    void 預期版本不符_丟ConcurrentUpdateException且不寫入任何事件() {
        String stream = newStreamId();
        store().append(stream, 0, List.of(created("一")));
        long before = lastPosition();

        assertThatThrownBy(() -> store().append(stream, 0, List.of(renamed("二"), thresholdSet("三"))))
                .isInstanceOf(ConcurrentUpdateException.class);
        assertThatThrownBy(() -> store().append(stream, 5, List.of(renamed("二"))))
                .isInstanceOf(ConcurrentUpdateException.class);

        assertThat(store().readStream(stream)).containsExactly(created("一"));
        assertThat(feed().readAfter(before, 10)).isEmpty();
    }

    @Test
    void 新串流的預期版本必須是0() {
        String stream = newStreamId();

        assertThatThrownBy(() -> store().append(stream, 1, List.of(created("一"))))
                .isInstanceOf(ConcurrentUpdateException.class);

        assertThat(store().readStream(stream)).isEmpty();
    }

    @Test
    void 兩條串流互不影響() {
        String first = newStreamId();
        String second = newStreamId();

        store().append(first, 0, List.of(created("一")));
        store().append(second, 0, List.of(created("二")));
        store().append(first, 1, List.of(renamed("三")));

        assertThat(store().readStream(first)).containsExactly(created("一"), renamed("三"));
        assertThat(store().readStream(second)).containsExactly(created("二"));
    }

    @Test
    void EventFeed依寫入順序給出遞增的位置() {
        long baseline = lastPosition();
        String first = newStreamId();
        String second = newStreamId();

        store().append(first, 0, List.of(created("一"), renamed("二")));
        store().append(second, 0, List.of(created("三")));

        List<RecordedEvent> recorded = feed().readAfter(baseline, 10);
        assertThat(recorded).extracting(RecordedEvent::event)
                .containsExactly(created("一"), renamed("二"), created("三"));
        assertThat(recorded).extracting(RecordedEvent::position).isSorted().doesNotHaveDuplicates();
        assertThat(recorded.get(0).position()).isGreaterThan(baseline);
    }

    @Test
    void readAfter只回傳位置之後的事件() {
        long baseline = lastPosition();
        store().append(newStreamId(), 0, List.of(created("一"), renamed("二"), thresholdSet("三")));
        List<RecordedEvent> all = feed().readAfter(baseline, 10);

        List<RecordedEvent> afterFirst = feed().readAfter(all.get(0).position(), 10);

        assertThat(afterFirst).extracting(RecordedEvent::event).containsExactly(renamed("二"), thresholdSet("三"));
        assertThat(feed().readAfter(all.get(2).position(), 10)).isEmpty();
    }

    @Test
    void maxCount限制回傳筆數() {
        long baseline = lastPosition();
        store().append(newStreamId(), 0, List.of(created("一"), renamed("二"), thresholdSet("三")));

        assertThat(feed().readAfter(baseline, 2)).extracting(RecordedEvent::event)
                .containsExactly(created("一"), renamed("二"));
    }

    @Test
    void 介面沒有更新與刪除() {
        // ES-2：事件紀錄只能新增
        assertThat(methodNames(EventStore.class)).containsExactlyInAnyOrder("readStream", "append");
        assertThat(methodNames(EventFeed.class)).containsExactlyInAnyOrder("readAfter");
    }

    private static List<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getMethods()).map(Method::getName).collect(Collectors.toList());
    }
}
