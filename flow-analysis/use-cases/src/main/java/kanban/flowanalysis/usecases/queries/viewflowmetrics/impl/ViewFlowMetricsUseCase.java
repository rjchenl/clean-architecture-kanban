package kanban.flowanalysis.usecases.queries.viewflowmetrics.impl;

import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetricsReader;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsInput;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsOutput;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsRequest;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsResponse;

/** 查詢：只讀流程分析的讀取模型，不載入聚合、不讀事件紀錄、不讀時鐘（CQRS-3）。 */
public final class ViewFlowMetricsUseCase implements ViewFlowMetricsInput {

    private static final ViewFlowMetricsResponse NOT_FOUND = new ViewFlowMetricsResponse(false, null);

    private final FlowMetricsReader metrics;
    private final ViewFlowMetricsOutput output;

    public ViewFlowMetricsUseCase(FlowMetricsReader metrics, ViewFlowMetricsOutput output) {
        this.metrics = metrics;
        this.output = output;
    }

    @Override
    public void execute(ViewFlowMetricsRequest request) {
        if (request == null || request.boardFlowId() == null || request.asOf() == null) {
            output.present(NOT_FOUND);
            return;
        }
        output.present(metrics.flowMetrics(request.boardFlowId(), request.asOf())
                .map(found -> new ViewFlowMetricsResponse(true, found))
                .orElse(NOT_FOUND));
    }
}
