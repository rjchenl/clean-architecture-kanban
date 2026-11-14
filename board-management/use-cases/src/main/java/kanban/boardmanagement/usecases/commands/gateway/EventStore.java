package kanban.boardmanagement.usecases.commands.gateway;

import java.util.List;
import kanban.boardmanagement.entities.board.BoardEvent;

/** 事件紀錄：寫入端唯一的事實來源（ES-1）。只能讀與新增，沒有更新與刪除（ES-2）。 */
public interface EventStore {

    /** 依寫入順序讀出一條串流的全部事件；串流不存在回傳空清單。 */
    List<BoardEvent> readStream(String streamId);

    /**
     * 把事件接在串流後面。
     *
     * @param expectedVersion 呼叫端認為這條串流目前有幾筆事件
     * @throws ConcurrentUpdateException 實際筆數與 expectedVersion 不符；此時一筆都不寫入（ES-6）
     */
    void append(String streamId, long expectedVersion, List<BoardEvent> events);
}
