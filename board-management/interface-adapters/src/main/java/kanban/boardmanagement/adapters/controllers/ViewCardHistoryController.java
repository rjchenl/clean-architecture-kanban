package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryInput;
import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryRequest;

/** Controller：卡片移動歷程。 */
public final class ViewCardHistoryController {

    private final ViewCardHistoryInput input;

    public ViewCardHistoryController(ViewCardHistoryInput input) {
        this.input = input;
    }

    public void handle(String boardId, String cardId) {
        input.execute(new ViewCardHistoryRequest(boardId, cardId));
    }
}
