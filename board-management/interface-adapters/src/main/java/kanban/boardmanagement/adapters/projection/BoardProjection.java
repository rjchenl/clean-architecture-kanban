package kanban.boardmanagement.adapters.projection;

import java.util.List;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.usecases.eventfeed.EventFeed;
import kanban.boardmanagement.usecases.eventfeed.RecordedEvent;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;

/**
 * Projection：讀取模型唯一的寫入途徑（CQRS-5）。
 *
 * <p>不是被動接收別人推過來的單一事件，而是自己從 EventFeed 依序讀出「上次處理位置之後」的事件，
 * 所以不會跳號、不會漏。每一筆事件各自一個交易；位置不比處理位置新的事件直接略過，
 * 所以同一筆事件處理兩次結果不變（冪等）。
 */
public final class BoardProjection {

    private static final int BATCH_SIZE = 200;

    private final EventFeed feed;
    private final ReadModelStore store;

    public BoardProjection(EventFeed feed, ReadModelStore store) {
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

    /** 清空讀取模型，從第一筆事件重播（ES-8）。 */
    public void rebuild() {
        store.inExclusiveTransaction(store::reset);
        catchUp();
    }

    private void applyOnce(RecordedEvent recorded) {
        if (recorded.position() <= store.checkpoint()) {
            return;
        }
        BoardEvent event = recorded.event();
        String boardId = event.boardId().value();
        BoardSnapshot before = store.boardById(boardId).orElse(null);

        CardHistoryProjector.cardIdOf(event).ifPresent(cardId -> {
            CardHistory current = store.cardHistory(boardId, cardId).orElse(null);
            CardHistoryProjector.apply(current, before, event)
                    .ifPresent(history -> store.saveCardHistory(boardId, history));
        });
        store.saveBoard(BoardSnapshotProjector.apply(before, event));
        store.advanceCheckpoint(recorded.position());
    }
}
