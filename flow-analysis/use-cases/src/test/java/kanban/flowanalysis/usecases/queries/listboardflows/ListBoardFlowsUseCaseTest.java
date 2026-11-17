package kanban.flowanalysis.usecases.queries.listboardflows;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import kanban.flowanalysis.usecases.queries.listboardflows.impl.ListBoardFlowsUseCase;
import org.junit.jupiter.api.Test;

class ListBoardFlowsUseCaseTest {

    private final List<ListBoardFlowsResponse> responses = new ArrayList<>();

    @Test
    void 清單依讀取模型的順序交回() {
        new ListBoardFlowsUseCase(() -> List.of("board-2", "board-1"), responses::add).execute();

        assertThat(responses).containsExactly(new ListBoardFlowsResponse(List.of("board-2", "board-1")));
    }

    @Test
    void 沒有任何看板流程_回空清單() {
        new ListBoardFlowsUseCase(List::of, responses::add).execute();

        assertThat(responses).containsExactly(new ListBoardFlowsResponse(List.of()));
    }
}
