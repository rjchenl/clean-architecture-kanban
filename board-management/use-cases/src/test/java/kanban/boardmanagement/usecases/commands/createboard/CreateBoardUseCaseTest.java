package kanban.boardmanagement.usecases.commands.createboard;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.board.*;
import kanban.boardmanagement.usecases.commands.CommandFixture;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.createboard.impl.CreateBoardUseCase;
import org.junit.jupiter.api.Test;

/** 指令 Use Case：建立看板。 */
class CreateBoardUseCaseTest extends CommandFixture {

    private final CreateBoardInput useCase = new CreateBoardUseCase(boards, time, ids, responses::add);

    @Test
    void 成功_看板被存起來且回覆新的識別碼() {
        useCase.execute(new CreateBoardRequest("新看板"));

        assertSucceeded("id-1");
        assertThat(boards.eventsOf(new BoardId("id-1"))).containsExactly(
                new BoardCreated(new BoardId("id-1"), new BoardName("新看板"), NOW));
    }

    @Test
    void 違反業務規則_回覆原因且沒有存入任何事件() {
        useCase.execute(new CreateBoardRequest("   "));

        assertFailed("NAME_BLANK");
        assertThat(boards.eventsOf(new BoardId("id-1"))).isEmpty();
    }

    @Test
    void 連續建立兩個看板_識別碼不同() {
        useCase.execute(new CreateBoardRequest("新看板"));
        useCase.execute(new CreateBoardRequest("第二個"));

        assertThat(responses).extracting(CommandResponse::createdId).containsExactly("id-1", "id-2");
    }

    @Test
    void Request欄位為null_回覆INVALID_REQUEST() {
        givenStandardBoard();

        useCase.execute(new CreateBoardRequest(null));

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
