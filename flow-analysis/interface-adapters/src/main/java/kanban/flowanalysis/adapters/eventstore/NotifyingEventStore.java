package kanban.flowanalysis.adapters.eventstore;

import java.util.List;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.usecases.commands.gateway.EventStore;

/**
 * 事件寫入成功後，同一個請求內通知一聲（用來叫流程分析的 Projection 補到最新）。
 * 通知失敗不影響寫入的結果：事件已經存好，讀取模型之後會補上（設計第 7.6 節）。
 */
public final class NotifyingEventStore implements EventStore {

    private static final System.Logger LOG = System.getLogger(NotifyingEventStore.class.getName());

    private final EventStore delegate;
    private final Runnable afterAppend;

    public NotifyingEventStore(EventStore delegate, Runnable afterAppend) {
        this.delegate = delegate;
        this.afterAppend = afterAppend;
    }

    @Override
    public List<BoardFlowEvent> readStream(String streamId) {
        return delegate.readStream(streamId);
    }

    @Override
    public void append(String streamId, long expectedVersion, List<BoardFlowEvent> events) {
        delegate.append(streamId, expectedVersion, events);
        try {
            afterAppend.run();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.ERROR,
                    "流程分析的事件已寫入，但讀取模型沒有更新成功；畫面會看到舊資料，直到下一次補讀成功為止", e);
        }
    }
}
