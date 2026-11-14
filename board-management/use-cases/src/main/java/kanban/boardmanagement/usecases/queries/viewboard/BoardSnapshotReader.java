package kanban.boardmanagement.usecases.queries.viewboard;

import java.util.Optional;

/** 讀取模型的唯讀 Gateway：看板現況。實作在外圈（CA-6）。 */
public interface BoardSnapshotReader {

    Optional<BoardSnapshot> boardById(String boardId);
}
