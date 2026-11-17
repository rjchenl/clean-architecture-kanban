package kanban.flowanalysis.adapters.eventstore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.usecases.commands.gateway.ConcurrentUpdateException;
import kanban.flowanalysis.usecases.commands.gateway.EventStore;
import kanban.flowanalysis.usecases.eventfeed.EventFeed;
import kanban.flowanalysis.usecases.eventfeed.RecordedEvent;

/** 記憶體版流程分析事件紀錄。檢查版本與寫入在同一個臨界區內。 */
public final class InMemoryEventStore implements EventStore, EventFeed {

    private final Map<String, List<BoardFlowEvent>> streams = new HashMap<>();
    private final List<RecordedEvent> all = new ArrayList<>();

    @Override
    public synchronized List<BoardFlowEvent> readStream(String streamId) {
        return List.copyOf(streams.getOrDefault(streamId, List.of()));
    }

    @Override
    public synchronized void append(String streamId, long expectedVersion, List<BoardFlowEvent> events) {
        List<BoardFlowEvent> stream = streams.getOrDefault(streamId, List.of());
        if (stream.size() != expectedVersion) {
            throw new ConcurrentUpdateException(streamId, expectedVersion);
        }
        List<BoardFlowEvent> extended = new ArrayList<>(stream);
        for (BoardFlowEvent event : events) {
            extended.add(event);
            all.add(new RecordedEvent(all.size() + 1L, event));
        }
        streams.put(streamId, extended);
    }

    @Override
    public synchronized List<RecordedEvent> readAfter(long position, int maxCount) {
        int from = (int) Math.min(Math.max(position, 0), all.size());
        int to = (int) Math.min((long) from + maxCount, all.size());
        return List.copyOf(all.subList(from, to));
    }
}
