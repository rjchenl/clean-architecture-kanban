package kanban.flowanalysis.frameworks.scheduling;

import kanban.flowanalysis.adapters.presenters.BoardFlowListViewModel;
import kanban.flowanalysis.adapters.presenters.CommandViewModel;

/**
 * 背景排程要的東西：每一種定時工作一個方法。介面定義在這一圈，由 Main 實作——
 * 排程不認識防腐層、Use Case、Controller 怎麼接（CA-8）。
 */
public interface FlowAnalysisBackgroundWork {

    /** 把看板動態同步到最新；遇到記錄不了的那一筆就停住，下一輪再試。 */
    void syncBoardActivities();

    /** 所有看板流程的識別碼。 */
    BoardFlowListViewModel listBoardFlows();

    /** 檢查一個看板流程有沒有工作項目待太久。 */
    CommandViewModel checkStuckWorkItems(String boardFlowId);
}
