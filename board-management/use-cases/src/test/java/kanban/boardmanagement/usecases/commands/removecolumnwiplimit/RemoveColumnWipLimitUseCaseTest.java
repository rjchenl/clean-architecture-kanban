package kanban.boardmanagement.usecases.commands.removecolumnwiplimit;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.removecolumnwiplimit.impl.RemoveColumnWipLimitUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：取消在製品上限。 */
class RemoveColumnWipLimitUseCaseTest extends CommandFixture {

    private final RemoveColumnWipLimitInput useCase = new RemoveColumnWipLimitUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new RemoveColumnWipLimitRequest("board-1", "col-doing"));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new ColumnWipLimitRemoved(BOARD, DOING, NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new RemoveColumnWipLimitRequest("no-such-board", "col-doing"));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new RemoveColumnWipLimitRequest("board-1", "col-missing"));

        assertFailed("COLUMN_NOT_FOUND");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new RemoveColumnWipLimitRequest("board-1", null));

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
