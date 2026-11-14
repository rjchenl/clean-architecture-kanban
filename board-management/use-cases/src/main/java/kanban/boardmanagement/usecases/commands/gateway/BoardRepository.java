package kanban.boardmanagement.usecases.commands.gateway;

import java.util.Optional;
import kanban.boardmanagement.entities.board.Board;
import kanban.boardmanagement.entities.board.BoardId;

/** 看板的 Repository：以聚合為單位（DDD-8）。介面在 Use Cases 圈，實作在外圈（CA-6）。 */
public interface BoardRepository {

    /** 找不到回傳 empty。 */
    Optional<Board> load(BoardId id);

    /**
     * 把聚合新產生的事件存起來。成功後聚合的未提交事件會被清空。
     *
     * @throws ConcurrentUpdateException 載入之後有別人先存了（樂觀鎖，ES-6）
     */
    void save(Board board);
}
