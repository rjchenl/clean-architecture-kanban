package kanban.boardmanagement.usecases.commands.archiveboard;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.archiveboard.impl.ArchiveBoardUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：封存看板。 */
class ArchiveBoardUseCaseTest extends CommandFixture {

    private final ArchiveBoardInput useCase = new ArchiveBoardUseCase(boards, time, responses::add);

    @Test
    void 成功_聚合被存回且回覆成功() {
        givenStandardBoard();

        useCase.execute(new ArchiveBoardRequest("board-1"));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(
                new BoardArchived(BOARD, NOW));
    }

    @Test
    void 看板不存在_回覆BOARD_NOT_FOUND() {
        givenStandardBoard();

        useCase.execute(new ArchiveBoardRequest("no-such-board"));

        assertFailed("BOARD_NOT_FOUND");
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        givenStandardBoard(new BoardArchived(BOARD, T0));
        int before = boards.eventsOf(BOARD).size();

        useCase.execute(new ArchiveBoardRequest("board-1"));

        assertFailed("BOARD_ARCHIVED");
        assertThat(boards.eventsOf(BOARD)).hasSize(before);
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new ArchiveBoardRequest(null));

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
