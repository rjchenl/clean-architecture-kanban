package kanban.flowanalysis.usecases.queries.viewflowmetrics;

/**
 * @param stage               階段的名稱：UNCLASSIFIED、NOT_STARTED、IN_PROGRESS、DONE
 * @param averageDwellSeconds 已經離開這一欄的停留時間平均（四捨五入到整數秒）；還沒有人離開過時為 null
 */
public record ColumnMetrics(String flowColumnId, String name, String stage, int workItemCount, Long averageDwellSeconds) {
}
