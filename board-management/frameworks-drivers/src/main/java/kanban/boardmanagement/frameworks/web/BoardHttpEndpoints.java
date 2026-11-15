package kanban.boardmanagement.frameworks.web;

import static kanban.boardmanagement.frameworks.web.HttpStatusMapping.respond;

import kanban.boardmanagement.adapters.presenters.BoardListViewModel;
import kanban.boardmanagement.adapters.presenters.BoardViewModel;
import kanban.boardmanagement.adapters.presenters.CardHistoryViewModel;
import kanban.boardmanagement.adapters.presenters.CommandViewModel;
import kanban.boardmanagement.frameworks.web.RequestBodies.CardColumnBody;
import kanban.boardmanagement.frameworks.web.RequestBodies.CardContentBody;
import kanban.boardmanagement.frameworks.web.RequestBodies.LimitBody;
import kanban.boardmanagement.frameworks.web.RequestBodies.NameBody;
import kanban.boardmanagement.frameworks.web.RequestBodies.NewCardBody;
import kanban.boardmanagement.frameworks.web.RequestBodies.PositionBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP 薄殼：每個端點只做一件事——把 HTTP 的輸入原樣轉給 BoardManagementRequests，再把拿回來的 ViewModel 原樣輸出。
 * 沒有業務判斷、沒有訊息文字、不認識 Use Case（CA-7、CA-9）。
 */
@RestController
@RequestMapping("/api/boards")
class BoardHttpEndpoints {

    private final BoardManagementRequests requests;

    BoardHttpEndpoints(BoardManagementRequests requests) {
        this.requests = requests;
    }

    // ---------------------------------------------------------------- 看板

    @PostMapping
    ResponseEntity<CommandViewModel> createBoard(@RequestBody NameBody body) {
        return respond(requests.createBoard(body.name()));
    }

    @GetMapping
    BoardListViewModel listBoards() {
        return requests.listBoards();
    }

    @GetMapping("/{boardId}")
    ResponseEntity<BoardViewModel> viewBoard(@PathVariable String boardId) {
        return respond(requests.viewBoard(boardId));
    }

    @PutMapping("/{boardId}/name")
    ResponseEntity<CommandViewModel> renameBoard(@PathVariable String boardId, @RequestBody NameBody body) {
        return respond(requests.renameBoard(boardId, body.name()));
    }

    @PostMapping("/{boardId}/archive")
    ResponseEntity<CommandViewModel> archiveBoard(@PathVariable String boardId) {
        return respond(requests.archiveBoard(boardId));
    }

    // ---------------------------------------------------------------- 欄位

    @PostMapping("/{boardId}/columns")
    ResponseEntity<CommandViewModel> addColumn(@PathVariable String boardId, @RequestBody NameBody body) {
        return respond(requests.addColumn(boardId, body.name()));
    }

    @PutMapping("/{boardId}/columns/{columnId}/name")
    ResponseEntity<CommandViewModel> renameColumn(@PathVariable String boardId, @PathVariable String columnId,
                                                  @RequestBody NameBody body) {
        return respond(requests.renameColumn(boardId, columnId, body.name()));
    }

    @PutMapping("/{boardId}/columns/{columnId}/position")
    ResponseEntity<CommandViewModel> moveColumn(@PathVariable String boardId, @PathVariable String columnId,
                                                @RequestBody PositionBody body) {
        return respond(requests.moveColumn(boardId, columnId, body.position()));
    }

    @PutMapping("/{boardId}/columns/{columnId}/wip-limit")
    ResponseEntity<CommandViewModel> setColumnWipLimit(@PathVariable String boardId, @PathVariable String columnId,
                                                       @RequestBody LimitBody body) {
        return respond(requests.setColumnWipLimit(boardId, columnId, body.limit()));
    }

    @DeleteMapping("/{boardId}/columns/{columnId}/wip-limit")
    ResponseEntity<CommandViewModel> removeColumnWipLimit(@PathVariable String boardId, @PathVariable String columnId) {
        return respond(requests.removeColumnWipLimit(boardId, columnId));
    }

    @DeleteMapping("/{boardId}/columns/{columnId}")
    ResponseEntity<CommandViewModel> removeColumn(@PathVariable String boardId, @PathVariable String columnId) {
        return respond(requests.removeColumn(boardId, columnId));
    }

    // ---------------------------------------------------------------- 卡片

    @PostMapping("/{boardId}/cards")
    ResponseEntity<CommandViewModel> addCard(@PathVariable String boardId, @RequestBody NewCardBody body) {
        return respond(requests.addCard(boardId, body.columnId(), body.title(), body.description()));
    }

    @PutMapping("/{boardId}/cards/{cardId}/content")
    ResponseEntity<CommandViewModel> changeCardContent(@PathVariable String boardId, @PathVariable String cardId,
                                                       @RequestBody CardContentBody body) {
        return respond(requests.changeCardContent(boardId, cardId, body.title(), body.description()));
    }

    @PutMapping("/{boardId}/cards/{cardId}/column")
    ResponseEntity<CommandViewModel> moveCard(@PathVariable String boardId, @PathVariable String cardId,
                                              @RequestBody CardColumnBody body) {
        return respond(requests.moveCard(boardId, cardId, body.columnId(), body.position()));
    }

    @PutMapping("/{boardId}/cards/{cardId}/position")
    ResponseEntity<CommandViewModel> repositionCard(@PathVariable String boardId, @PathVariable String cardId,
                                                    @RequestBody PositionBody body) {
        return respond(requests.repositionCard(boardId, cardId, body.position()));
    }

    @DeleteMapping("/{boardId}/cards/{cardId}")
    ResponseEntity<CommandViewModel> removeCard(@PathVariable String boardId, @PathVariable String cardId) {
        return respond(requests.removeCard(boardId, cardId));
    }

    @GetMapping("/{boardId}/cards/{cardId}/history")
    ResponseEntity<CardHistoryViewModel> viewCardHistory(@PathVariable String boardId, @PathVariable String cardId) {
        return respond(requests.viewCardHistory(boardId, cardId));
    }
}
