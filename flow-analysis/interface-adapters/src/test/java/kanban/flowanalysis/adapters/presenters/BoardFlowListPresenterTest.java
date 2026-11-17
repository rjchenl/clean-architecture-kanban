package kanban.flowanalysis.adapters.presenters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kanban.flowanalysis.usecases.queries.listboardflows.ListBoardFlowsResponse;
import org.junit.jupiter.api.Test;

class BoardFlowListPresenterTest {

    @Test
    void 識別碼清單原樣交出() {
        BoardFlowListPresenter presenter = new BoardFlowListPresenter();

        presenter.present(new ListBoardFlowsResponse(List.of("board-2", "board-1")));

        assertThat(presenter.viewModel()).isEqualTo(new BoardFlowListViewModel(List.of("board-2", "board-1")));
    }
}
