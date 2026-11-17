package kanban.flowanalysis.usecases.commands.checkstuckworkitems.impl;

import kanban.flowanalysis.usecases.commands.checkstuckworkitems.CheckStuckWorkItemsInput;
import kanban.flowanalysis.usecases.commands.checkstuckworkitems.CheckStuckWorkItemsOutput;
import kanban.flowanalysis.usecases.commands.checkstuckworkitems.CheckStuckWorkItemsRequest;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;
import kanban.flowanalysis.usecases.commands.support.BoardFlowCommand;

/** 定時檢查：從時間來源取得現在的時間交給看板流程，由它判斷哪些工作項目卡住（ES-4、F10）。 */
public final class CheckStuckWorkItemsUseCase implements CheckStuckWorkItemsInput {

    private final BoardFlowRepository flows;
    private final TimeSource time;
    private final CheckStuckWorkItemsOutput output;

    public CheckStuckWorkItemsUseCase(BoardFlowRepository flows, TimeSource time, CheckStuckWorkItemsOutput output) {
        this.flows = flows;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(CheckStuckWorkItemsRequest request) {
        output.present(BoardFlowCommand.onExistingBoardFlow(flows,
                () -> BoardFlowCommand.required(request.boardFlowId()),
                flow -> {
                    flow.checkStuckWorkItems(time.now());
                    return null;
                }));
    }
}
