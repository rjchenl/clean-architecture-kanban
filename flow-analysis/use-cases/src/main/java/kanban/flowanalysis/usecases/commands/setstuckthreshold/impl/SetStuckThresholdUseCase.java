package kanban.flowanalysis.usecases.commands.setstuckthreshold.impl;

import kanban.flowanalysis.entities.boardflow.StuckThreshold;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;
import kanban.flowanalysis.usecases.commands.setstuckthreshold.SetStuckThresholdInput;
import kanban.flowanalysis.usecases.commands.setstuckthreshold.SetStuckThresholdOutput;
import kanban.flowanalysis.usecases.commands.setstuckthreshold.SetStuckThresholdRequest;
import kanban.flowanalysis.usecases.commands.support.BoardFlowCommand;

public final class SetStuckThresholdUseCase implements SetStuckThresholdInput {

    private final BoardFlowRepository flows;
    private final TimeSource time;
    private final SetStuckThresholdOutput output;

    public SetStuckThresholdUseCase(BoardFlowRepository flows, TimeSource time, SetStuckThresholdOutput output) {
        this.flows = flows;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(SetStuckThresholdRequest request) {
        output.present(BoardFlowCommand.onExistingBoardFlow(flows,
                () -> BoardFlowCommand.required(request.boardFlowId(), request.days()),
                flow -> {
                    flow.setStuckThreshold(new StuckThreshold(request.days()), time.now());
                    return null;
                }));
    }
}
