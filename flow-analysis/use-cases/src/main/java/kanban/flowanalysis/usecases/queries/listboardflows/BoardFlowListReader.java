package kanban.flowanalysis.usecases.queries.listboardflows;

import java.util.List;

/** 讀看板流程清單（讀取模型）。 */
public interface BoardFlowListReader {

    /** 依第一次出現的順序。 */
    List<String> allBoardFlowIds();
}
