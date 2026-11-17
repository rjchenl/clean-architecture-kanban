package kanban.flowanalysis.usecases.commands.setcolumnstage.impl;

import kanban.flowanalysis.entities.boardflow.FlowColumnId;
import kanban.flowanalysis.entities.boardflow.Stage;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;
import kanban.flowanalysis.usecases.commands.setcolumnstage.SetColumnStageInput;
import kanban.flowanalysis.usecases.commands.setcolumnstage.SetColumnStageOutput;
import kanban.flowanalysis.usecases.commands.setcolumnstage.SetColumnStageRequest;
import kanban.flowanalysis.usecases.commands.support.BoardFlowCommand;

public final class SetColumnStageUseCase implements SetColumnStageInput {

    private final BoardFlowRepository flows;
    private final TimeSource time;
    private final SetColumnStageOutput output;

    public SetColumnStageUseCase(BoardFlowRepository flows, TimeSource time, SetColumnStageOutput output) {
        this.flows = flows;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(SetColumnStageRequest request) {
        output.present(BoardFlowCommand.onExistingBoardFlow(flows,
                () -> BoardFlowCommand.required(request.boardFlowId(), request.flowColumnId(), request.stage()),
                flow -> {
                    flow.setColumnStage(new FlowColumnId(request.flowColumnId()), Stage.named(request.stage()), time.now());
                    return null;
                }));
    }
}
