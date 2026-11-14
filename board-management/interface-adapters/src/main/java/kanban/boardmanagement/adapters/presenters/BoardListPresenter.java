package kanban.boardmanagement.adapters.presenters;

import kanban.boardmanagement.usecases.queries.listboards.ListBoardsOutput;
import kanban.boardmanagement.usecases.queries.listboards.ListBoardsResponse;

/** Presenter：看板清單（CA-9）。 */
public final class BoardListPresenter implements ListBoardsOutput {

    private BoardListViewModel viewModel;

    @Override
    public void present(ListBoardsResponse response) {
        viewModel = new BoardListViewModel(response.boards().stream()
                .map(board -> new BoardListItemViewModel(board.boardId(), board.name(), board.archived(),
                        Labels.archived(board.archived())))
                .toList());
    }

    public BoardListViewModel viewModel() {
        if (viewModel == null) {
            throw new IllegalStateException("Use Case 還沒有交出結果");
        }
        return viewModel;
    }
}
