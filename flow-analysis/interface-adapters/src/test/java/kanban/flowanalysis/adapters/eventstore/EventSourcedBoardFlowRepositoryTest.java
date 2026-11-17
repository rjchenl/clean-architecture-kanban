package kanban.flowanalysis.adapters.eventstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import kanban.flowanalysis.entities.boardflow.ActivityPosition;
import kanban.flowanalysis.entities.boardflow.BoardFlow;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import kanban.flowanalysis.entities.boardflow.StuckThreshold;
import kanban.flowanalysis.usecases.commands.gateway.ConcurrentUpdateException;
import org.junit.jupiter.api.Test;

/** 看板流程 Repository：載入＝重播事件，儲存＝帶預期版本號新增事件（ES-4、ES-6）。 */
class EventSourcedBoardFlowRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");
    private static final BoardFlowId FLOW = new BoardFlowId("board-1");

    private final InMemoryEventStore events = new InMemoryEventStore();
    private final EventSourcedBoardFlowRepository repository = new EventSourcedBoardFlowRepository(events);

    @Test
    void 找不到_回empty() {
        assertThat(repository.load(FLOW)).isEmpty();
    }

    @Test
    void 存了再載入_狀態相同() {
        BoardFlow created = BoardFlow.create(FLOW, new ActivityPosition(1), new FlowBoardName("產品開發"), T0);
        repository.save(created);

        BoardFlow loaded = repository.load(FLOW).orElseThrow();

        assertThat(loaded.id()).isEqualTo(FLOW);
        assertThat(loaded.persistedVersion()).isEqualTo(1);
        assertThat(created.uncommittedEvents()).as("存好之後未提交事件清空").isEmpty();
        assertThat(events.readStream("board-flow-board-1"))
                .containsExactly(new BoardFlowCreated(FLOW, new ActivityPosition(1), new FlowBoardName("產品開發"), T0));
    }

    @Test
    void 版本衝突_丟ConcurrentUpdateException() {
        repository.save(BoardFlow.create(FLOW, new ActivityPosition(1), new FlowBoardName("產品開發"), T0));
        BoardFlow first = repository.load(FLOW).orElseThrow();
        BoardFlow second = repository.load(FLOW).orElseThrow();
        first.setStuckThreshold(new StuckThreshold(3), T0);
        second.setStuckThreshold(new StuckThreshold(5), T0);
        repository.save(first);

        assertThatThrownBy(() -> repository.save(second)).isInstanceOf(ConcurrentUpdateException.class);
        assertThat(events.readStream("board-flow-board-1")).hasSize(2);
    }

    @Test
    void 沒有新事件_不寫入() {
        repository.save(BoardFlow.create(FLOW, new ActivityPosition(1), new FlowBoardName("產品開發"), T0));
        BoardFlow loaded = repository.load(FLOW).orElseThrow();

        repository.save(loaded);

        assertThat(events.readAfter(0, 10)).hasSize(1);
    }

    @Test
    void 不同看板流程的串流分開() {
        repository.save(BoardFlow.create(FLOW, new ActivityPosition(1), new FlowBoardName("甲"), T0));
        repository.save(BoardFlow.create(new BoardFlowId("board-2"), new ActivityPosition(2), new FlowBoardName("乙"), T0));

        assertThat(events.readStream("board-flow-board-1")).hasSize(1);
        assertThat(events.readStream("board-flow-board-2")).hasSize(1);
        assertThat(List.of(repository.load(FLOW).orElseThrow().id(), repository.load(new BoardFlowId("board-2")).orElseThrow().id()))
                .containsExactly(FLOW, new BoardFlowId("board-2"));
    }
}
