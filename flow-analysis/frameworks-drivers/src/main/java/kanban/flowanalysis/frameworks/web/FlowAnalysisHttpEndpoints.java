package kanban.flowanalysis.frameworks.web;

import static kanban.flowanalysis.frameworks.web.FlowAnalysisHttpStatusMapping.respond;

import kanban.flowanalysis.adapters.presenters.CommandViewModel;
import kanban.flowanalysis.adapters.presenters.FlowMetricsViewModel;
import kanban.flowanalysis.frameworks.web.FlowAnalysisRequestBodies.StageBody;
import kanban.flowanalysis.frameworks.web.FlowAnalysisRequestBodies.StuckThresholdBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程分析的 HTTP 薄殼：每個端點只把 HTTP 的輸入原樣轉給 FlowAnalysisRequests，再把拿回來的 ViewModel 原樣輸出。
 * 沒有業務判斷、沒有訊息文字、不認識 Use Case（CA-7、CA-9）。網址用看板的識別碼，它也是看板流程的識別碼。
 */
@RestController
@RequestMapping("/api/flow-analysis/boards")
class FlowAnalysisHttpEndpoints {

    private final FlowAnalysisRequests requests;

    FlowAnalysisHttpEndpoints(FlowAnalysisRequests requests) {
        this.requests = requests;
    }

    @GetMapping("/{boardId}")
    ResponseEntity<FlowMetricsViewModel> viewFlowMetrics(@PathVariable String boardId) {
        return respond(requests.viewFlowMetrics(boardId));
    }

    @PutMapping("/{boardId}/columns/{columnId}/stage")
    ResponseEntity<CommandViewModel> setColumnStage(@PathVariable String boardId, @PathVariable String columnId,
            @RequestBody StageBody body) {
        return respond(requests.setColumnStage(boardId, columnId, body.stage()));
    }

    @PutMapping("/{boardId}/stuck-threshold")
    ResponseEntity<CommandViewModel> setStuckThreshold(@PathVariable String boardId,
            @RequestBody StuckThresholdBody body) {
        return respond(requests.setStuckThreshold(boardId, body.days()));
    }

    @DeleteMapping("/{boardId}/stuck-threshold")
    ResponseEntity<CommandViewModel> removeStuckThreshold(@PathVariable String boardId) {
        return respond(requests.removeStuckThreshold(boardId));
    }
}
