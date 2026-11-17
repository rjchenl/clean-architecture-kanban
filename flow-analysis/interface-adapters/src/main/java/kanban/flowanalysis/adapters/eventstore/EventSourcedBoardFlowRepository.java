package kanban.flowanalysis.adapters.eventstore;

import java.util.List;
import java.util.Optional;
import kanban.flowanalysis.entities.boardflow.BoardFlow;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.EventStore;

/** 看板流程的 Repository：載入＝重播事件串流（ES-4），儲存＝帶預期版本號新增事件（ES-6）。沒有另存現況（ES-1）。 */
public final class EventSourcedBoardFlowRepository implements BoardFlowRepository {

    private final EventStore store;

    public EventSourcedBoardFlowRepository(EventStore store) {
        this.store = store;
    }

    @Override
    public Optional<BoardFlow> load(BoardFlowId id) {
        List<BoardFlowEvent> history = store.readStream(streamIdOf(id));
        return history.isEmpty() ? Optional.empty() : Optional.of(BoardFlow.rehydrate(history));
    }

    @Override
    public void save(BoardFlow flow) {
        List<BoardFlowEvent> events = flow.uncommittedEvents();
        if (events.isEmpty()) {
            return;
        }
        store.append(streamIdOf(flow.id()), flow.persistedVersion(), events);
        flow.markCommitted();
    }

    private static String streamIdOf(BoardFlowId id) {
        return "board-flow-" + id.value();
    }
}
