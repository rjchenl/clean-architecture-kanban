package kanban.boardmanagement.adapters.projection;

import kanban.boardmanagement.usecases.queries.listboards.BoardSummaryReader;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshotReader;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryReader;

/**
 * 讀取模型的儲存。查詢 Use Case 只看得到它繼承的三個唯讀介面；
 * 這裡多出來的寫入方法只有 Projection 可以呼叫（CQRS-5），而且只能在 inExclusiveTransaction 之內。
 */
public interface ReadModelStore extends BoardSnapshotReader, BoardSummaryReader, CardHistoryReader {

    /** Projection 已經處理到事件紀錄的哪個位置；還沒處理過任何事件時為 0。 */
    long checkpoint();

    /** 同一時間只有一個在執行；work 內的寫入與 checkpoint 更新一起成功，丟例外就一起回復。 */
    void inExclusiveTransaction(Runnable work);

    void saveBoard(BoardSnapshot board);

    void saveCardHistory(String boardId, CardHistory history);

    void advanceCheckpoint(long position);

    /** 清空全部讀取模型並把 checkpoint 設回 0（ES-8：查詢端資料可隨時丟掉重建）。 */
    void reset();
}
