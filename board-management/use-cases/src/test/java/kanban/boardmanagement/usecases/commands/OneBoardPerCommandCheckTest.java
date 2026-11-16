package kanban.boardmanagement.usecases.commands;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import org.junit.jupiter.api.Test;

/**
 * DDD-5 的行為檢查本身會咬人：一次指令存了兩個看板，FakeBoardRepository 的檢查必須失敗。
 * 每個指令 Use Case 測試（繼承 CommandFixture）結束時都會自動跑這個檢查；這支測試刻意不繼承它，才能直接驗檢查本身。
 */
class OneBoardPerCommandCheckTest {

    private static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");
    private static final BoardId BOARD_A = new BoardId("board-a");
    private static final BoardId BOARD_B = new BoardId("board-b");

    private final FakeBoardRepository boards = new FakeBoardRepository();

    @Test
    void 一次指令存了兩個看板_檢查失敗() {
        givenBoards();

        // 違規樣本：同一次指令載入並存回兩個看板
        boards.save(boards.load(BOARD_A).orElseThrow());
        boards.save(boards.load(BOARD_B).orElseThrow());

        assertThatThrownBy(boards::assertAtMostOneBoardSaved)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("DDD-5");
    }

    @Test
    void 一次指令只存一個看板_檢查通過() {
        givenBoards();

        boards.save(boards.load(BOARD_A).orElseThrow());

        assertThatCode(boards::assertAtMostOneBoardSaved).doesNotThrowAnyException();
    }

    @Test
    void 兩次指令各存一個看板_檢查通過() {
        givenBoards();

        boards.save(boards.load(BOARD_A).orElseThrow());
        boards.commandFinished();
        boards.save(boards.load(BOARD_B).orElseThrow());

        assertThatCode(boards::commandFinished).doesNotThrowAnyException();
    }

    @Test
    void 指令測試的共用場景在收到回覆時自動檢查() {
        CommandFixture commandTest = new CommandFixture() {
        };
        commandTest.boards.given(BOARD_A, List.of(new BoardCreated(BOARD_A, new BoardName("甲"), T0)));
        commandTest.boards.given(BOARD_B, List.of(new BoardCreated(BOARD_B, new BoardName("乙"), T0)));

        // 違規樣本：一個指令存了兩個看板之後才回覆
        commandTest.boards.save(commandTest.boards.load(BOARD_A).orElseThrow());
        commandTest.boards.save(commandTest.boards.load(BOARD_B).orElseThrow());

        assertThatThrownBy(() -> commandTest.responses.add(CommandResponse.success(null)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("DDD-5");
    }

    private void givenBoards() {
        boards.given(BOARD_A, List.of(new BoardCreated(BOARD_A, new BoardName("甲"), T0)));
        boards.given(BOARD_B, List.of(new BoardCreated(BOARD_B, new BoardName("乙"), T0)));
    }
}
