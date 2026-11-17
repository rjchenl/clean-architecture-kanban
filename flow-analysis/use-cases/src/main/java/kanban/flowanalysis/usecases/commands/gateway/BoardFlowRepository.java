package kanban.flowanalysis.usecases.commands.gateway;

import java.util.Optional;
import kanban.flowanalysis.entities.boardflow.BoardFlow;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;

/** 看板流程的 Repository：一個聚合一個（DDD-8）。介面在這一圈，實作在 Interface Adapters 圈（CA-6）。 */
public interface BoardFlowRepository {

    /** 找不到回傳 empty。 */
    Optional<BoardFlow> load(BoardFlowId id);

    /**
     * 把看板流程新產生的事件存起來。成功後它的未提交事件會被清空。
     *
     * @throws ConcurrentUpdateException 載入之後有別人先存了（樂觀鎖，ES-6）
     */
    void save(BoardFlow flow);
}
