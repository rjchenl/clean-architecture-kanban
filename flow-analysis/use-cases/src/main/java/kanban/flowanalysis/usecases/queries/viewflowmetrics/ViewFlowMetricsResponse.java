package kanban.flowanalysis.usecases.queries.viewflowmetrics;

/** @param found 找不到時為 false、metrics 為 null（例如看板剛建立、還沒同步過來） */
public record ViewFlowMetricsResponse(boolean found, FlowMetrics metrics) {
}
