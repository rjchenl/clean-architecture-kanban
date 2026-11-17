package kanban.flowanalysis.adapters.projection;

import java.util.List;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.entities.boardflow.WorkItemFlowResultChanged;
import kanban.flowanalysis.usecases.eventfeed.EventFeed;
import kanban.flowanalysis.usecases.eventfeed.RecordedEvent;

/**
 * 流程分析的 Projection：讀取模型唯一的寫入途徑（CQRS-5）。
 *
 * <p>自己從 EventFeed 依序讀「上次處理位置之後」的事件，每一筆一個交易；位置不比處理位置新的直接略過（冪等）。
 * 只照事件記下的值更新、做加總與計數，「算不算完成、算不算卡住」都由聚合判斷好寫在事件裡（DDD-9）。
 */
public final class FlowProjection {

    private static final int BATCH_SIZE = 200;

    private final EventFeed feed;
    private final ReadModelStore store;

    public FlowProjection(EventFeed feed, ReadModelStore store) {
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
        BoardFlowEvent event = recorded.event();
        String boardFlowId = event.boardFlowId().value();
        FlowBoardDocument before = store.boardFlowDocument(boardFlowId).orElse(null);
        store.saveBoardFlow(FlowBoardProjector.apply(before, event));
        if (event instanceof WorkItemFlowResultChanged changed) {
            if (changed.completedAt() != null) {
                store.saveCompletion(boardFlowId, changed.workItemId().value(), changed.completedAt(),
                        Math.round(changed.leadTime().value().toMillis() / 1000.0));
            } else {
                store.removeCompletion(boardFlowId, changed.workItemId().value());
            }
        }
        store.advanceCheckpoint(recorded.position());
    }
}
