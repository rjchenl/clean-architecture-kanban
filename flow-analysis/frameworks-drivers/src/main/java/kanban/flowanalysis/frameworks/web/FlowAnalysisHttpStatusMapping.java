package kanban.flowanalysis.frameworks.web;

import kanban.flowanalysis.adapters.presenters.CommandOutcome;
import kanban.flowanalysis.adapters.presenters.CommandViewModel;
import kanban.flowanalysis.adapters.presenters.FlowMetricsViewModel;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/** 把 ViewModel 上已經決定好的結果，一對一換成 HTTP 狀態碼（和看板管理同一套對應）。這裡沒有任何判斷邏輯。 */
final class FlowAnalysisHttpStatusMapping {

    private FlowAnalysisHttpStatusMapping() {
    }

    static int statusOf(CommandOutcome outcome) {
        return switch (outcome) {
            case SUCCEEDED -> 200;
            case REJECTED -> 422;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
        };
    }

    static ResponseEntity<CommandViewModel> respond(CommandViewModel viewModel) {
        return ResponseEntity.status(HttpStatusCode.valueOf(statusOf(viewModel.outcome()))).body(viewModel);
    }

    static ResponseEntity<FlowMetricsViewModel> respond(FlowMetricsViewModel viewModel) {
        return ResponseEntity.status(HttpStatusCode.valueOf(viewModel.found() ? 200 : 404)).body(viewModel);
    }
}
