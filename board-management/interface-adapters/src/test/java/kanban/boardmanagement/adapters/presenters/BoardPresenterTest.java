package kanban.boardmanagement.adapters.presenters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.CardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.ColumnSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardResponse;
import org.junit.jupiter.api.Test;

class BoardPresenterTest {

    private final BoardPresenter presenter = new BoardPresenter();

    private static BoardSnapshot board(boolean archived) {
        return new BoardSnapshot("board-1", "產品開發", archived, List.of(
                new ColumnSnapshot("col-todo", "待辦", null, List.of(
                        new CardSnapshot("card-1", "設計登入頁", "含忘記密碼"),
                        new CardSnapshot("card-2", "寫用語表", ""))),
                new ColumnSnapshot("col-doing", "進行中", 3, List.of(
                        new CardSnapshot("card-3", "串接金流", ""),
                        new CardSnapshot("card-4", "寫架構測試", ""))),
                new ColumnSnapshot("col-done", "完成", 5, List.of())));
    }

    @Test
    void 整個看板整理成可直接顯示的資料() {
        presenter.present(new ViewBoardResponse(true, board(false)));

        assertThat(presenter.viewModel()).isEqualTo(new BoardViewModel(true, "board-1", "產品開發", false, "", List.of(
                new ColumnViewModel("col-todo", "待辦", "2", List.of(
                        new CardViewModel("card-1", "設計登入頁", "含忘記密碼"),
                        new CardViewModel("card-2", "寫用語表", ""))),
                new ColumnViewModel("col-doing", "進行中", "2 / 3", List.of(
                        new CardViewModel("card-3", "串接金流", ""),
                        new CardViewModel("card-4", "寫架構測試", ""))),
                new ColumnViewModel("col-done", "完成", "0 / 5", List.of()))));
    }

    @Test
    void 有上限顯示2斜線3() {
        presenter.present(new ViewBoardResponse(true, board(false)));

        assertThat(presenter.viewModel().columns().get(1).cardCountLabel()).isEqualTo("2 / 3");
    }

    @Test
    void 無上限只顯示張數() {
        presenter.present(new ViewBoardResponse(true, board(false)));

        assertThat(presenter.viewModel().columns().get(0).cardCountLabel()).isEqualTo("2");
    }

    @Test
    void 已封存顯示標籤() {
        presenter.present(new ViewBoardResponse(true, board(true)));

        assertThat(presenter.viewModel().archived()).isTrue();
        assertThat(presenter.viewModel().archivedLabel()).isEqualTo("已封存");
    }

    @Test
    void 找不到看板_found為false且其他欄位為空() {
        presenter.present(new ViewBoardResponse(false, null));

        assertThat(presenter.viewModel()).isEqualTo(new BoardViewModel(false, "", "", false, "", List.of()));
    }
}
