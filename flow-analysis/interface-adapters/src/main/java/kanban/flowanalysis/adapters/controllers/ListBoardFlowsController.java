package kanban.flowanalysis.adapters.controllers;

import kanban.flowanalysis.usecases.queries.listboardflows.ListBoardFlowsInput;

public final class ListBoardFlowsController {

    private final ListBoardFlowsInput input;

    public ListBoardFlowsController(ListBoardFlowsInput input) {
        this.input = input;
    }

    public void handle() {
        input.execute();
    }
}
