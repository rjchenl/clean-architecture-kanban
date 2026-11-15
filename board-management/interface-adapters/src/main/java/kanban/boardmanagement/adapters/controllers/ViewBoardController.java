package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardInput;
import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardRequest;

/** Controller：看板現況。 */
public final class ViewBoardController {

    private final ViewBoardInput input;

    public ViewBoardController(ViewBoardInput input) {
        this.input = input;
    }

    public void handle(String boardId) {
        input.execute(new ViewBoardRequest(boardId));
    }
}
