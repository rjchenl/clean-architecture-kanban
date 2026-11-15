package kanban.boardmanagement.frameworks.web;

import kanban.boardmanagement.adapters.presenters.BoardViewModel;
import kanban.boardmanagement.adapters.presenters.CardHistoryViewModel;
import kanban.boardmanagement.adapters.presenters.CommandOutcome;
import kanban.boardmanagement.adapters.presenters.CommandViewModel;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/** 把 ViewModel 上已經決定好的結果，一對一換成 HTTP 狀態碼。這裡沒有任何判斷邏輯。 */
final class HttpStatusMapping {

    private HttpStatusMapping() {
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

    static ResponseEntity<BoardViewModel> respond(BoardViewModel viewModel) {
        return ResponseEntity.status(HttpStatusCode.valueOf(viewModel.found() ? 200 : 404)).body(viewModel);
    }

    static ResponseEntity<CardHistoryViewModel> respond(CardHistoryViewModel viewModel) {
        return ResponseEntity.status(HttpStatusCode.valueOf(viewModel.found() ? 200 : 404)).body(viewModel);
    }
}
