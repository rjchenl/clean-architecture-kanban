package kanban.boardmanagement.adapters.eventstore;

import java.util.List;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.usecases.commands.gateway.EventStore;

/**
 * 事件寫入成功後，同一個請求內通知一聲（用來叫 Projection 補到最新）。
 * 通知失敗不影響寫入的結果：事件已經存好，讀取模型之後會補上（設計文件 7.5）。
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
    public List<BoardEvent> readStream(String streamId) {
        return delegate.readStream(streamId);
    }

    @Override
    public void append(String streamId, long expectedVersion, List<BoardEvent> events) {
        delegate.append(streamId, expectedVersion, events);
        try {
            afterAppend.run();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "事件已寫入，但寫入後的通知失敗；讀取模型會在下次補上", e);
        }
    }
}
