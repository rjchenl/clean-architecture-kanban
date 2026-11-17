package kanban.flowanalysis.usecases.commands.gateway;

import java.util.List;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;

/**
 * 流程分析的事件紀錄（寫入端唯一的事實來源，ES-1）：只能新增，不能修改或刪除（ES-2）。
 * 和看板管理的事件紀錄分開，介面長得像但各自演變（意外的重複，DDD-2）。
 */
public interface EventStore {

    /** 依寫入順序讀出一條串流的全部事件；串流不存在回傳空清單。 */
    List<BoardFlowEvent> readStream(String streamId);

    /**
     * @param expectedVersion 呼叫端認為這條串流目前有幾筆事件
     * @throws ConcurrentUpdateException 實際筆數與 expectedVersion 不符；此時一筆都不寫入（ES-6）
     */
    void append(String streamId, long expectedVersion, List<BoardFlowEvent> events);
}
