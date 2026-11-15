package kanban.boardmanagement.usecases.commands.changecardcontent;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.changecardcontent.impl.ChangeCardContentUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：修改卡片內容。 */
class ChangeCardContentUseCaseTest extends CommandFixture {

    private final ChangeCardContentInput useCase = new ChangeCardContentUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new ChangeCardContentRequest("board-1", "card-1", "新標題", "新說明"));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new CardContentChanged(BOARD, CARD_1, new CardTitle("新標題"), new CardDescription("新說明"), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new ChangeCardContentRequest("no-such-board", "card-1", "新標題", "新說明"));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new ChangeCardContentRequest("board-1", "card-1", " ", "新說明"));

        assertFailed("NAME_BLANK");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new ChangeCardContentRequest("board-1", null, "新標題", "新說明"));

        assertFailed("INVALID_REQUEST");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void Request本身為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(null);

        assertFailed("INVALID_REQUEST");
    }

    @Test
    void 說明為null_視為沒有說明() {
        givenStandardBoard();

        useCase.execute(new ChangeCardContentRequest("board-1", "card-1", "新標題", null));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new CardContentChanged(BOARD, CARD_1, new CardTitle("新標題"), new CardDescription(""), NOW));
    }
}
