package kanban.flowanalysis.usecases.queries.viewflowmetrics;

/**
 * 最近 8 週內完成的工作項目的交付時間。
 *
 * @param averageLeadTimeSeconds 四捨五入到整數秒；沒有完成的時候為 null
 * @param longestLeadTimeSeconds 沒有完成的時候為 null
 */
public record Delivery(int completedCount, Long averageLeadTimeSeconds, Long longestLeadTimeSeconds) {
}
