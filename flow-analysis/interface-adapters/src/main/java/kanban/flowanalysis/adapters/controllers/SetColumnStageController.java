package kanban.flowanalysis.adapters.controllers;

import kanban.flowanalysis.usecases.commands.setcolumnstage.SetColumnStageInput;
import kanban.flowanalysis.usecases.commands.setcolumnstage.SetColumnStageRequest;

public final class SetColumnStageController {

    private final SetColumnStageInput input;

    public SetColumnStageController(SetColumnStageInput input) {
        this.input = input;
    }

    public void handle(String boardFlowId, String flowColumnId, String stage) {
        input.execute(new SetColumnStageRequest(boardFlowId, flowColumnId, stage));
    }
}
