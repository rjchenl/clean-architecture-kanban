package kanban.flowanalysis.usecases.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kanban.flowanalysis.entities.boardflow.BoardFlow;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.ConcurrentUpdateException;

/** 測試用的記憶體版看板流程 Repository：保存事件、含樂觀鎖。Use Case 測試不啟動 Spring（CA-3）。 */
public final class FakeBoardFlowRepository implements BoardFlowRepository {

    private final Map<BoardFlowId, List<BoardFlowEvent>> streams = new HashMap<>();
    private final Set<BoardFlowId> savedFlows = new LinkedHashSet<>();
    private boolean conflictOnNextSave;

    /** DDD-5：一個指令只修改一個看板流程。FlowCommandFixture 在每個指令回覆時、以及每個測試結束時自動呼叫。 */
    public void assertAtMostOneFlowSaved() {
        if (savedFlows.size() > 1) {
            throw new AssertionError("DDD-5 一個指令只能修改一個看板流程，這次存了 " + savedFlows.size() + " 個：" + savedFlows);
        }
    }

    public void commandFinished() {
        assertAtMostOneFlowSaved();
        savedFlows.clear();
    }

    public void given(BoardFlowId id, List<BoardFlowEvent> history) {
        streams.put(id, new ArrayList<>(history));
    }

    public List<BoardFlowEvent> eventsOf(BoardFlowId id) {
        return List.copyOf(streams.getOrDefault(id, List.of()));
    }

    public void conflictOnNextSave() {
        conflictOnNextSave = true;
    }

    @Override
    public Optional<BoardFlow> load(BoardFlowId id) {
        List<BoardFlowEvent> history = streams.get(id);
        return history == null ? Optional.empty() : Optional.of(BoardFlow.rehydrate(history));
    }

    @Override
    public void save(BoardFlow flow) {
        savedFlows.add(flow.id());
        List<BoardFlowEvent> stream = streams.computeIfAbsent(flow.id(), id -> new ArrayList<>());
        if (conflictOnNextSave || stream.size() != flow.persistedVersion()) {
            conflictOnNextSave = false;
            throw new ConcurrentUpdateException("board-flow-" + flow.id().value(), flow.persistedVersion());
        }
        stream.addAll(flow.uncommittedEvents());
        flow.markCommitted();
    }
}
