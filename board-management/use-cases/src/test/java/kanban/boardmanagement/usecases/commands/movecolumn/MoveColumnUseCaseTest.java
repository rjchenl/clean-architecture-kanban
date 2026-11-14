package kanban.boardmanagement.usecases.commands.movecolumn;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.movecolumn.impl.MoveColumnUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：調整欄位順序。 */
class MoveColumnUseCaseTest extends CommandFixture {

    private final MoveColumnInput useCase = new MoveColumnUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new MoveColumnRequest("board-1", "col-todo", 1));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new ColumnMoved(BOARD, TODO, new Position(1), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new MoveColumnRequest("no-such-board", "col-todo", 1));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new MoveColumnRequest("board-1", "col-todo", -1));

        assertFailed("POSITION_OUT_OF_RANGE");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new MoveColumnRequest("board-1", null, 1));

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
