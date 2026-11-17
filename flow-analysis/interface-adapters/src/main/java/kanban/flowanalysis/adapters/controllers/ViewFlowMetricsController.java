package kanban.flowanalysis.adapters.controllers;

import java.time.Instant;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsInput;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsRequest;

public final class ViewFlowMetricsController {

    private final ViewFlowMetricsInput input;

    public ViewFlowMetricsController(ViewFlowMetricsInput input) {
        this.input = input;
    }

    /** @param asOf 「最近 8 週」從哪個時間點往回算（由 Main 帶入現在的時間） */
    public void handle(String boardFlowId, Instant asOf) {
        input.execute(new ViewFlowMetricsRequest(boardFlowId, asOf));
    }
}
