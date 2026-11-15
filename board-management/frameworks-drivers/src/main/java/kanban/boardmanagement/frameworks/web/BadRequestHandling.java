package kanban.boardmanagement.frameworks.web;

import kanban.boardmanagement.adapters.presenters.CommandViewModel;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 請求內容讀不懂（不是合法 JSON、型別不對）時的統一回覆。只處理這一種情況；
 * 其他例外是程式錯誤或系統故障，照常回 500，不包裝成「請求不完整」。
 * 回覆的內容與訊息向 BoardManagementRequests 要，薄殼自己不寫任何文字。
 */
@RestControllerAdvice
class BadRequestHandling {

    private final BoardManagementRequests requests;

    BadRequestHandling(BoardManagementRequests requests) {
        this.requests = requests;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<CommandViewModel> unreadableRequest() {
        return HttpStatusMapping.respond(requests.invalidRequest());
    }
}
