package kanban.boardmanagement.usecases.commands.removecard;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.removecard.impl.RemoveCardUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：刪除卡片。 */
class RemoveCardUseCaseTest extends CommandFixture {

    private final RemoveCardInput useCase = new RemoveCardUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new RemoveCardRequest("board-1", "card-1"));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new CardRemoved(BOARD, CARD_1, TODO, NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new RemoveCardRequest("no-such-board", "card-1"));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new RemoveCardRequest("board-1", "card-missing"));

        assertFailed("CARD_NOT_FOUND");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new RemoveCardRequest("board-1", null));

        assertFailed("INVALID_REQUEST");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void Request本身為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(null);

        assertFailed("INVALID_REQUEST");
    }
}
