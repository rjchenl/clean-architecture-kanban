package kanban.boardmanagement.usecases.commands.movecard;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.movecard.impl.MoveCardUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：移動卡片。 */
class MoveCardUseCaseTest extends CommandFixture {

    private final MoveCardInput useCase = new MoveCardUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new MoveCardRequest("board-1", "card-2", "col-todo", 0));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new CardMoved(BOARD, CARD_2, DOING, TODO, new Position(0), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new MoveCardRequest("no-such-board", "card-2", "col-todo", 0));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new MoveCardRequest("board-1", "card-1", "col-doing", 0));

        assertFailed("WIP_LIMIT_EXCEEDED");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new MoveCardRequest("board-1", "card-2", null, 0));

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
    void 儲存時版本衝突_回覆CONCURRENT_UPDATE() {
        givenStandardBoard();
        boards.conflictOnNextSave();

        useCase.execute(new MoveCardRequest("board-1", "card-2", "col-todo", 0));

        assertFailed("CONCURRENT_UPDATE");
        assertThat(newEvents()).isEmpty();
    }
}
