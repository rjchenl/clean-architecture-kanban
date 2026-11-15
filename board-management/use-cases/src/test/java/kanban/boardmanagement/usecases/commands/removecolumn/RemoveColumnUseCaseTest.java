package kanban.boardmanagement.usecases.commands.removecolumn;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.removecolumn.impl.RemoveColumnUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：刪除欄位。 */
class RemoveColumnUseCaseTest extends CommandFixture {

    private final RemoveColumnInput useCase = new RemoveColumnUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard(new ColumnAdded(BOARD, new ColumnId("col-done"), new ColumnName("完成"), T0));

        useCase.execute(new RemoveColumnRequest("board-1", "col-done"));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new ColumnRemoved(BOARD, new ColumnId("col-done"), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new RemoveColumnRequest("no-such-board", "col-todo"));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new RemoveColumnRequest("board-1", "col-todo"));

        assertFailed("COLUMN_NOT_EMPTY");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new RemoveColumnRequest("board-1", null));

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
