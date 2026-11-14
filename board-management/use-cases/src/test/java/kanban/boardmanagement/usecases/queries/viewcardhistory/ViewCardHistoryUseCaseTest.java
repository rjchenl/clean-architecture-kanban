package kanban.boardmanagement.usecases.queries.viewcardhistory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kanban.boardmanagement.usecases.queries.viewcardhistory.impl.ViewCardHistoryUseCase;
import org.junit.jupiter.api.Test;

/** 查詢 Use Case：卡片移動歷程。 */
class ViewCardHistoryUseCaseTest {

    private static final Instant AT = Instant.parse("2026-11-14T01:00:00Z");
    private static final CardHistory HISTORY = new CardHistory("card-1", "設計登入頁", List.of(
            new CardHistoryEntry(CardHistoryEntryKind.ADDED, null, "待辦", AT),
            new CardHistoryEntry(CardHistoryEntryKind.MOVED, "待辦", "進行中", AT.plusSeconds(60)),
            new CardHistoryEntry(CardHistoryEntryKind.REMOVED, "進行中", null, AT.plusSeconds(120))));

    private final List<ViewCardHistoryResponse> responses = new ArrayList<>();
    private final ViewCardHistoryInput useCase = new ViewCardHistoryUseCase(
            (boardId, cardId) -> boardId.equals("board-1") && cardId.equals("card-1")
                    ? Optional.of(HISTORY) : Optional.empty(),
            responses::add);

    @Test
    void 找得到_把讀取模型的資料原樣交給Output() {
        useCase.execute(new ViewCardHistoryRequest("board-1", "card-1"));

        assertThat(responses).containsExactly(new ViewCardHistoryResponse(true, HISTORY));
    }

    @Test
    void 找不到卡片或看板_found為false() {
        useCase.execute(new ViewCardHistoryRequest("board-1", "no-such-card"));
        useCase.execute(new ViewCardHistoryRequest("no-such-board", "card-1"));

        assertThat(responses).containsExactly(
                new ViewCardHistoryResponse(false, null), new ViewCardHistoryResponse(false, null));
    }

    @Test
    void request為null或欄位為null_found為false() {
        useCase.execute(null);
        useCase.execute(new ViewCardHistoryRequest("board-1", null));
        useCase.execute(new ViewCardHistoryRequest(null, "card-1"));

        assertThat(responses).hasSize(3).containsOnly(new ViewCardHistoryResponse(false, null));
    }
}
