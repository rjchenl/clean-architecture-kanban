package kanban.boardmanagement.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import kanban.boardmanagement.entities.board.Board;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.movecard.MoveCardRequest;
import kanban.boardmanagement.usecases.commands.movecard.impl.MoveCardUseCase;
import kanban.boardmanagement.usecases.commands.movecolumn.MoveColumnRequest;
import kanban.boardmanagement.usecases.commands.movecolumn.impl.MoveColumnUseCase;
import kanban.boardmanagement.usecases.commands.repositioncard.RepositionCardRequest;
import kanban.boardmanagement.usecases.commands.repositioncard.impl.RepositionCardUseCase;
import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.SetColumnWipLimitRequest;
import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.impl.SetColumnWipLimitUseCase;
import org.junit.jupiter.api.Test;

/**
 * 只有「預期內的失敗」才轉成失敗原因；程式錯誤與儲存設備故障要照常往外丟，
 * 不可以被包裝成「請求內容不完整」（那會誤導使用者，也會把 bug 藏起來）。
 */
class UnexpectedFailuresTest extends CommandFixture {

    /** 載入時就故障的 Repository。 */
    private static BoardRepository failingWith(RuntimeException failure) {
        return new BoardRepository() {
            @Override
            public Optional<Board> load(BoardId id) {
                throw failure;
            }

            @Override
            public void save(Board board) {
                throw failure;
            }
        };
    }

    @Test
    void 儲存端丟NullPointerException_照常往外丟() {
        MoveCardUseCase useCase = new MoveCardUseCase(failingWith(new NullPointerException("bug")), time, responses::add);

        assertThatThrownBy(() -> useCase.execute(new MoveCardRequest("board-1", "card-1", "col-doing", 0)))
                .isInstanceOf(NullPointerException.class);
        assertThat(responses).isEmpty();
    }

    @Test
    void 儲存端丟IllegalArgumentException_照常往外丟() {
        MoveCardUseCase useCase =
                new MoveCardUseCase(failingWith(new IllegalArgumentException("bug")), time, responses::add);

        assertThatThrownBy(() -> useCase.execute(new MoveCardRequest("board-1", "card-1", "col-doing", 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(responses).isEmpty();
    }

    @Test
    void 數字欄位沒給_回覆INVALID_REQUEST() {
        givenStandardBoard();

        new MoveCardUseCase(boards, time, responses::add).execute(new MoveCardRequest("board-1", "card-1", "col-doing", null));
        new RepositionCardUseCase(boards, time, responses::add).execute(new RepositionCardRequest("board-1", "card-1", null));
        new MoveColumnUseCase(boards, time, responses::add).execute(new MoveColumnRequest("board-1", "col-todo", null));
        new SetColumnWipLimitUseCase(boards, time, responses::add)
                .execute(new SetColumnWipLimitRequest("board-1", "col-todo", null));

        assertThat(responses).hasSize(4).containsOnly(new CommandResponse(false, "INVALID_REQUEST", null));
        assertThat(newEvents()).isEmpty();
    }
}
