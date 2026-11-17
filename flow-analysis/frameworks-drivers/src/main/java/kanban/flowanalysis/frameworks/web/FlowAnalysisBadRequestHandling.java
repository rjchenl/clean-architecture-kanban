package kanban.flowanalysis.frameworks.web;

import kanban.flowanalysis.adapters.presenters.CommandViewModel;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 流程分析的請求內容讀不懂（不是合法 JSON、型別不對、沒有內容）時的統一回覆。只管流程分析自己的端點；
 * 其他例外是程式錯誤或系統故障，照常回 500。回覆的內容與訊息向 FlowAnalysisRequests 要，薄殼自己不寫任何文字。
 */
@RestControllerAdvice(basePackageClasses = FlowAnalysisHttpEndpoints.class)
class FlowAnalysisBadRequestHandling {

    private final FlowAnalysisRequests requests;

    FlowAnalysisBadRequestHandling(FlowAnalysisRequests requests) {
        this.requests = requests;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<CommandViewModel> unreadableRequest() {
        return FlowAnalysisHttpStatusMapping.respond(requests.invalidRequest());
    }
}
