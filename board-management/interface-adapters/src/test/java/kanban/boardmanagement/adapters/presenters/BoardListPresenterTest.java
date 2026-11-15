package kanban.boardmanagement.adapters.presenters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kanban.boardmanagement.usecases.queries.listboards.BoardSummary;
import kanban.boardmanagement.usecases.queries.listboards.ListBoardsResponse;
import org.junit.jupiter.api.Test;

class BoardListPresenterTest {

    private final BoardListPresenter presenter = new BoardListPresenter();

    @Test
    void 看板清單整理成可直接顯示的資料() {
        presenter.present(new ListBoardsResponse(List.of(
                new BoardSummary("board-1", "產品開發", false),
                new BoardSummary("board-2", "舊專案", true))));

        assertThat(presenter.viewModel()).isEqualTo(new BoardListViewModel(List.of(
                new BoardListItemViewModel("board-1", "產品開發", false, ""),
                new BoardListItemViewModel("board-2", "舊專案", true, "已封存"))));
    }

    @Test
    void 沒有看板_空清單() {
        presenter.present(new ListBoardsResponse(List.of()));

        assertThat(presenter.viewModel().boards()).isEmpty();
    }
}
