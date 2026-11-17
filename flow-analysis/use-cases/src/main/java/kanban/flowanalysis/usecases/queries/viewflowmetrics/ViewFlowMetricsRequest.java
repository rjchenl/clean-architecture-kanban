package kanban.flowanalysis.usecases.queries.viewflowmetrics;

import java.time.Instant;

/**
 * @param asOf 「最近 8 週」從哪個時間點往回算；由呼叫端帶入，查詢 Use Case 不讀時鐘
 */
public record ViewFlowMetricsRequest(String boardFlowId, Instant asOf) {
}
