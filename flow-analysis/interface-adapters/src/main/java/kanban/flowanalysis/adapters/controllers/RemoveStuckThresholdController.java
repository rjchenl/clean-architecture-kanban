package kanban.flowanalysis.adapters.controllers;

import kanban.flowanalysis.usecases.commands.removestuckthreshold.RemoveStuckThresholdInput;
import kanban.flowanalysis.usecases.commands.removestuckthreshold.RemoveStuckThresholdRequest;

public final class RemoveStuckThresholdController {

    private final RemoveStuckThresholdInput input;

    public RemoveStuckThresholdController(RemoveStuckThresholdInput input) {
        this.input = input;
    }

    public void handle(String boardFlowId) {
        input.execute(new RemoveStuckThresholdRequest(boardFlowId));
    }
}
