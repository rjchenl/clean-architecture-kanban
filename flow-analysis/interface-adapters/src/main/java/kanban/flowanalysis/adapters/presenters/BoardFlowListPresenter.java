package kanban.flowanalysis.adapters.presenters;

import kanban.flowanalysis.usecases.queries.listboardflows.ListBoardFlowsOutput;
import kanban.flowanalysis.usecases.queries.listboardflows.ListBoardFlowsResponse;

public final class BoardFlowListPresenter implements ListBoardFlowsOutput {

    private BoardFlowListViewModel viewModel;

    @Override
    public void present(ListBoardFlowsResponse response) {
        viewModel = new BoardFlowListViewModel(response.boardFlowIds());
    }

    public BoardFlowListViewModel viewModel() {
        if (viewModel == null) {
            throw new IllegalStateException("Use Case 還沒有交出結果");
        }
        return viewModel;
    }
}
