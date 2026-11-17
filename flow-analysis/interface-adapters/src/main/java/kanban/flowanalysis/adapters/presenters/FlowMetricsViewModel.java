package kanban.flowanalysis.adapters.presenters;

import java.util.List;

/**
 * 流程分析頁的 ViewModel（CA-9）：只有可以直接顯示的字串、布林與它們的清單。
 *
 * @param found           看板剛建立、還沒同步過來時為 false，畫面顯示 notFoundMessage
 * @param stuckThresholdDays 輸入框的預設值；沒設定時是空字串
 */
public record FlowMetricsViewModel(boolean found, String boardFlowId, String boardName, String archivedLabel,
        boolean hasStuckThreshold, String stuckThresholdDays, String stuckThresholdLabel,
        List<StageOptionViewModel> stageOptions, List<FlowColumnViewModel> columns, String deliverySummary,
        List<WeekViewModel> weeks, List<StuckWorkItemViewModel> stuckWorkItems, String notFoundMessage) {
}
