package kanban.boardmanagement.adapters.presenters;

import java.util.List;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.ColumnSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardOutput;
import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardResponse;

/**
 * Presenter：看板現況（CA-9）。只做格式整理。
 * 這裡不比較張數與上限、不標示「已滿」——那是業務判斷，只有看板聚合能做（DDD-9）。
 */
public final class BoardPresenter implements ViewBoardOutput {

    private BoardViewModel viewModel;

    @Override
    public void present(ViewBoardResponse response) {
        if (!response.found()) {
            viewModel = new BoardViewModel(false, "", "", false, "", List.of());
            return;
        }
        BoardSnapshot board = response.board();
        viewModel = new BoardViewModel(true, board.boardId(), board.name(), board.archived(),
                Labels.archived(board.archived()),
                board.columns().stream().map(BoardPresenter::column).toList());
    }

    public BoardViewModel viewModel() {
        if (viewModel == null) {
            throw new IllegalStateException("Use Case 還沒有交出結果");
        }
        return viewModel;
    }

    private static ColumnViewModel column(ColumnSnapshot column) {
        String count = String.valueOf(column.cards().size());
        String label = column.wipLimit() == null ? count : count + " / " + column.wipLimit();
        return new ColumnViewModel(column.columnId(), column.name(), label,
                column.cards().stream()
                        .map(card -> new CardViewModel(card.cardId(), card.title(), card.description()))
                        .toList());
    }
}
