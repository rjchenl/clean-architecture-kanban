package kanban.flowanalysis.frameworks.scheduling;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * 背景排程（設計第 8 節）：只決定「什麼時候做」，做什麼全部轉給 FlowAnalysisBackgroundWork（CA-7）。
 * 間隔由設定值調整；沒有加 @Component，由 Main 依設定 kanban.flow-analysis.background 決定要不要建立。
 * 某一輪丟出例外時，Spring 會寫日誌並在下一個間隔照常再跑。
 */
public final class FlowAnalysisSchedule {

    private final FlowAnalysisBackgroundWork work;

    public FlowAnalysisSchedule(FlowAnalysisBackgroundWork work) {
        this.work = work;
    }

    @Scheduled(fixedDelayString = "${kanban.flow-analysis.sync-interval}")
    void sync() {
        work.syncBoardActivities();
    }

    @Scheduled(fixedDelayString = "${kanban.flow-analysis.stuck-check-interval}")
    void checkStuck() {
        for (String boardFlowId : work.listBoardFlows().boardFlowIds()) {
            work.checkStuckWorkItems(boardFlowId);
        }
    }
}
