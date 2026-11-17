package kanban.flowanalysis.adapters.controllers;

import kanban.flowanalysis.usecases.commands.checkstuckworkitems.CheckStuckWorkItemsInput;
import kanban.flowanalysis.usecases.commands.checkstuckworkitems.CheckStuckWorkItemsRequest;

public final class CheckStuckWorkItemsController {

    private final CheckStuckWorkItemsInput input;

    public CheckStuckWorkItemsController(CheckStuckWorkItemsInput input) {
        this.input = input;
    }

    public void handle(String boardFlowId) {
        input.execute(new CheckStuckWorkItemsRequest(boardFlowId));
    }
}
