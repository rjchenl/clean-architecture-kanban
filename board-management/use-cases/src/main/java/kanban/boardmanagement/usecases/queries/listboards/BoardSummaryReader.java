package kanban.boardmanagement.usecases.queries.listboards;

import java.util.List;

/** 讀取模型的唯讀 Gateway：看板清單，依建立順序。實作在外圈（CA-6）。 */
public interface BoardSummaryReader {

    List<BoardSummary> allBoards();
}
