package kanban.boardmanagement.usecases.commands.addcard;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.addcard.impl.AddCardUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：新增卡片。 */
class AddCardUseCaseTest extends CommandFixture {

    private final AddCardInput useCase = new AddCardUseCase(boards, time, ids, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new AddCardRequest("board-1", "col-todo", "新卡片", "說明"));

        assertSucceeded("id-1");
        assertThat(newEvents()).containsExactly(
                new CardAdded(BOARD, new CardId("id-1"), TODO, new CardTitle("新卡片"), new CardDescription("說明"), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new AddCardRequest("no-such-board", "col-todo", "新卡片", "說明"));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new AddCardRequest("board-1", "col-doing", "新卡片", ""));

        assertFailed("WIP_LIMIT_EXCEEDED");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new AddCardRequest("board-1", "col-todo", null, "說明"));

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

        useCase.execute(new AddCardRequest("board-1", "col-todo", "新卡片", null));

        assertSucceeded("id-1");
        assertThat(newEvents()).containsExactly(
                new CardAdded(BOARD, new CardId("id-1"), TODO, new CardTitle("新卡片"), new CardDescription(""), NOW));
    }
}
