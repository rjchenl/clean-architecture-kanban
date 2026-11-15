package kanban.boardmanagement.usecases.commands.repositioncard;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.repositioncard.impl.RepositionCardUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：欄內排序。 */
class RepositionCardUseCaseTest extends CommandFixture {

    private final RepositionCardInput useCase = new RepositionCardUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard(new CardAdded(BOARD, new CardId("card-3"), TODO, new CardTitle("第三張"), new CardDescription(""), T0));

        useCase.execute(new RepositionCardRequest("board-1", "card-3", 0));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new CardRepositioned(BOARD, new CardId("card-3"), TODO, new Position(0), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new RepositionCardRequest("no-such-board", "card-1", 0));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new RepositionCardRequest("board-1", "card-1", 9));

        assertFailed("POSITION_OUT_OF_RANGE");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new RepositionCardRequest("board-1", null, 0));

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
