package kanban.boardmanagement.usecases.queries.viewboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kanban.boardmanagement.usecases.queries.viewboard.impl.ViewBoardUseCase;
import org.junit.jupiter.api.Test;

/** 查詢 Use Case：看板現況。只讀讀取模型，不啟動 Spring（CA-3、CQRS-3）。 */
class ViewBoardUseCaseTest {

    private static final BoardSnapshot SNAPSHOT = new BoardSnapshot("board-1", "產品開發", false, List.of(
            new ColumnSnapshot("col-todo", "待辦", null, List.of(new CardSnapshot("card-1", "第一張", ""))),
            new ColumnSnapshot("col-doing", "進行中", 1, List.of())));

    private final List<ViewBoardResponse> responses = new ArrayList<>();
    private final ViewBoardInput useCase = new ViewBoardUseCase(
            boardId -> boardId.equals("board-1") ? Optional.of(SNAPSHOT) : Optional.empty(),
            responses::add);

    @Test
    void 找得到_把讀取模型的資料原樣交給Output() {
        useCase.execute(new ViewBoardRequest("board-1"));

        assertThat(responses).containsExactly(new ViewBoardResponse(true, SNAPSHOT));
    }

    @Test
    void 找不到_found為false() {
        useCase.execute(new ViewBoardRequest("no-such-board"));

        assertThat(responses).containsExactly(new ViewBoardResponse(false, null));
    }

    @Test
    void request為null或欄位為null_found為false() {
        useCase.execute(null);
        useCase.execute(new ViewBoardRequest(null));

        assertThat(responses).containsExactly(new ViewBoardResponse(false, null), new ViewBoardResponse(false, null));
    }
}
