package kanban.flowanalysis.adapters.controllers;

import kanban.flowanalysis.usecases.commands.setstuckthreshold.SetStuckThresholdInput;
import kanban.flowanalysis.usecases.commands.setstuckthreshold.SetStuckThresholdRequest;

public final class SetStuckThresholdController {

    private final SetStuckThresholdInput input;

    public SetStuckThresholdController(SetStuckThresholdInput input) {
        this.input = input;
    }

    public void handle(String boardFlowId, Integer days) {
        input.execute(new SetStuckThresholdRequest(boardFlowId, days));
    }
}
