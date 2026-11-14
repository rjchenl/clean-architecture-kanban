package kanban.boardmanagement.usecases.commands.addcolumn;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.addcolumn.impl.AddColumnUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：新增欄位。 */
class AddColumnUseCaseTest extends CommandFixture {

    private final AddColumnInput useCase = new AddColumnUseCase(boards, time, ids, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new AddColumnRequest("board-1", "完成"));

        assertSucceeded("id-1");
        assertThat(newEvents()).containsExactly(
                new ColumnAdded(BOARD, new ColumnId("id-1"), new ColumnName("完成"), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new AddColumnRequest("no-such-board", "完成"));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new AddColumnRequest("board-1", "待辦"));

        assertFailed("COLUMN_NAME_DUPLICATED");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new AddColumnRequest("board-1", null));

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
