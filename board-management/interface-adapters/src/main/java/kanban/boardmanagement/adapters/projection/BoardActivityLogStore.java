package kanban.boardmanagement.adapters.projection;

import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityReader;

/**
 * 看板動態紀錄的儲存：讀取模型，只由 {@link BoardActivityProjection} 寫入（CQRS-5），可以清掉重建（ES-8）。
 * 有自己的處理位置，和看板現況、卡片歷程那份讀取模型分開，所以上線時會從第一筆事件補齊。
 */
public interface BoardActivityLogStore extends BoardActivityReader {

    /** 已經處理到事件紀錄的哪個位置；還沒處理過任何事件時為 0。 */
    long checkpoint();

    /** 同一時間只有一個在執行；work 內的寫入與 checkpoint 更新一起成功，丟例外就一起回復。 */
    void inExclusiveTransaction(Runnable work);

    /** 同一個位置再寫一次會覆蓋，結果不變。 */
    void append(BoardActivity activity);

    void advanceCheckpoint(long position);

    /** 清空看板動態紀錄並把 checkpoint 設回 0。 */
    void reset();
}
