package kanban.flowanalysis.usecases.queries.viewflowmetrics;

import java.util.List;

/**
 * 一個看板的流程數字（讀取模型，CQRS-3）。數字都是讀取模型對聚合已經判斷好的結果做的加總、平均、計數（設計第 0 節 4c）。
 *
 * @param stuckThresholdDays 沒設定卡住門檻時為 null
 * @param columns            沒被刪除的欄位，依畫面順序
 * @param recentWeeks        最近 8 週（含本週），由舊到新
 */
public record FlowMetrics(String boardFlowId, String boardName, boolean archived, Integer stuckThresholdDays,
        List<ColumnMetrics> columns, List<WeeklyCompletion> recentWeeks, Delivery delivery,
        List<StuckWorkItem> stuckWorkItems) {
}
