package kanban.boardmanagement.usecases.commands.setcolumnwiplimit;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.impl.SetColumnWipLimitUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：設定在製品上限。 */
class SetColumnWipLimitUseCaseTest extends CommandFixture {

    private final SetColumnWipLimitInput useCase = new SetColumnWipLimitUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new SetColumnWipLimitRequest("board-1", "col-todo", 3));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new ColumnWipLimitSet(BOARD, TODO, new WipLimit(3), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new SetColumnWipLimitRequest("no-such-board", "col-todo", 3));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new SetColumnWipLimitRequest("board-1", "col-todo", 0));

        assertFailed("WIP_LIMIT_NOT_POSITIVE");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new SetColumnWipLimitRequest("board-1", null, 3));

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
