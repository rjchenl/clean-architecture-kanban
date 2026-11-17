package kanban.flowanalysis.usecases.commands.removestuckthreshold.impl;

import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;
import kanban.flowanalysis.usecases.commands.removestuckthreshold.RemoveStuckThresholdInput;
import kanban.flowanalysis.usecases.commands.removestuckthreshold.RemoveStuckThresholdOutput;
import kanban.flowanalysis.usecases.commands.removestuckthreshold.RemoveStuckThresholdRequest;
import kanban.flowanalysis.usecases.commands.support.BoardFlowCommand;

public final class RemoveStuckThresholdUseCase implements RemoveStuckThresholdInput {

    private final BoardFlowRepository flows;
    private final TimeSource time;
    private final RemoveStuckThresholdOutput output;

    public RemoveStuckThresholdUseCase(BoardFlowRepository flows, TimeSource time, RemoveStuckThresholdOutput output) {
        this.flows = flows;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(RemoveStuckThresholdRequest request) {
        output.present(BoardFlowCommand.onExistingBoardFlow(flows,
                () -> BoardFlowCommand.required(request.boardFlowId()),
                flow -> {
                    flow.removeStuckThreshold(time.now());
                    return null;
                }));
    }
}
