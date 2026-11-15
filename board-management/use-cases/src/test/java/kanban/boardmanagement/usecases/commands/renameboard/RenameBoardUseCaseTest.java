package kanban.boardmanagement.usecases.commands.renameboard;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.renameboard.impl.RenameBoardUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：看板改名。 */
class RenameBoardUseCaseTest extends CommandFixture {

    private final RenameBoardInput useCase = new RenameBoardUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new RenameBoardRequest("board-1", "新名稱"));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new BoardRenamed(BOARD, new BoardName("新名稱"), NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new RenameBoardRequest("no-such-board", "新名稱"));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard();
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new RenameBoardRequest("board-1", ""));

        assertFailed("NAME_BLANK");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new RenameBoardRequest("board-1", null));

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
