package kanban.flowanalysis.usecases.queries.listboardflows.impl;

import kanban.flowanalysis.usecases.queries.listboardflows.BoardFlowListReader;
import kanban.flowanalysis.usecases.queries.listboardflows.ListBoardFlowsInput;
import kanban.flowanalysis.usecases.queries.listboardflows.ListBoardFlowsOutput;
import kanban.flowanalysis.usecases.queries.listboardflows.ListBoardFlowsResponse;

/** 查詢：只讀流程分析的讀取模型（CQRS-3）。 */
public final class ListBoardFlowsUseCase implements ListBoardFlowsInput {

    private final BoardFlowListReader flows;
    private final ListBoardFlowsOutput output;

    public ListBoardFlowsUseCase(BoardFlowListReader flows, ListBoardFlowsOutput output) {
        this.flows = flows;
        this.output = output;
    }

    @Override
    public void execute() {
        output.present(new ListBoardFlowsResponse(flows.allBoardFlowIds()));
    }
}
