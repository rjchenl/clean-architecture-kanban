package kanban.boardmanagement.usecases.commands.renamecolumn;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.renamecolumn.impl.RenameColumnUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：欄位改名。 */
class RenameColumnUseCaseTest extends CommandFixture {

    private final RenameColumnInput useCase = new RenameColumnUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new RenameColumnRequest("board-1", "col-todo", "準備中"));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new ColumnRenamed(BOARD, TODO, new ColumnName("準備中"), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new RenameColumnRequest("no-such-board", "col-todo", "準備中"));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new RenameColumnRequest("board-1", "col-todo", "進行中"));

        assertFailed("COLUMN_NAME_DUPLICATED");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new RenameColumnRequest("board-1", null, "準備中"));

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
