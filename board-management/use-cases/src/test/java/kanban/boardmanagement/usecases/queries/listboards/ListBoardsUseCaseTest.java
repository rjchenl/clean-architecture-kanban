package kanban.boardmanagement.usecases.queries.listboards;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import kanban.boardmanagement.usecases.queries.listboards.impl.ListBoardsUseCase;
import org.junit.jupiter.api.Test;

/** 查詢 Use Case：看板清單。 */
class ListBoardsUseCaseTest {

    private final List<ListBoardsResponse> responses = new ArrayList<>();

    @Test
    void 有看板_依讀取模型給的順序原樣交給Output() {
        List<BoardSummary> boards = List.of(
                new BoardSummary("board-1", "產品開發", false),
                new BoardSummary("board-2", "舊專案", true));
        ListBoardsInput useCase = new ListBoardsUseCase(() -> boards, responses::add);

        useCase.execute();

        assertThat(responses).containsExactly(new ListBoardsResponse(boards));
    }

    @Test
    void 沒有看板_回空清單() {
        ListBoardsInput useCase = new ListBoardsUseCase(List::of, responses::add);

        useCase.execute();

        assertThat(responses).containsExactly(new ListBoardsResponse(List.of()));
    }
}
