package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.queries.listboards.ListBoardsInput;

/** Controller：看板清單。 */
public final class ListBoardsController {

    private final ListBoardsInput input;

    public ListBoardsController(ListBoardsInput input) {
        this.input = input;
    }

    public void handle() {
        input.execute();
    }
}
