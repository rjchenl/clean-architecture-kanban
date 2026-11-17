package kanban.boardmanagement.adapters.projection;

import java.util.List;
import kanban.boardmanagement.usecases.eventfeed.EventFeed;
import kanban.boardmanagement.usecases.eventfeed.RecordedEvent;

/**
 * 看板動態的 Projection：把看板管理的領域事件翻成公布格式，存進看板動態紀錄（CQRS-5）。
 *
 * <p>和 {@link BoardProjection} 一樣自己從 EventFeed 依序讀「上次處理位置之後」的事件，每筆一個交易，
 * 位置不比處理位置新的直接略過（冪等）。處理位置是自己的，所以第一次上線時會從第一筆事件補齊，
 * 第一階段以來的歷史全部都會公布。
 */
public final class BoardActivityProjection {

    private static final int BATCH_SIZE = 200;

    private final EventFeed feed;
    private final BoardActivityLogStore store;

    public BoardActivityProjection(EventFeed feed, BoardActivityLogStore store) {
        this.feed = feed;
        this.store = store;
    }

    /** 補到最新。遇到處理不了的事件就停在前一筆並把例外往外丟。 */
    public void catchUp() {
        while (true) {
            long before = store.checkpoint();
            List<RecordedEvent> batch = feed.readAfter(before, BATCH_SIZE);
            for (RecordedEvent recorded : batch) {
                store.inExclusiveTransaction(() -> applyOnce(recorded));
            }
            if (batch.isEmpty() || store.checkpoint() == before) {
                return;
            }
        }
    }

    /** 清空看板動態紀錄，從第一筆事件重播（ES-8）。位置是來源事件的全域位置，重建後完全一樣。 */
    public void rebuild() {
        store.inExclusiveTransaction(store::reset);
        catchUp();
    }

    private void applyOnce(RecordedEvent recorded) {
        if (recorded.position() <= store.checkpoint()) {
            return;
        }
        BoardActivityFormat.of(recorded).ifPresent(store::append);
        store.advanceCheckpoint(recorded.position());
    }
}
