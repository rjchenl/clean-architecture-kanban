package kanban.boardmanagement.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.entities.board.CardAdded;
import kanban.boardmanagement.entities.board.CardDescription;
import kanban.boardmanagement.entities.board.CardId;
import kanban.boardmanagement.entities.board.CardTitle;
import kanban.boardmanagement.entities.board.ColumnAdded;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.entities.board.ColumnWipLimitSet;
import kanban.boardmanagement.entities.board.WipLimit;
import kanban.boardmanagement.usecases.commands.gateway.IdentityGenerator;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import org.junit.jupiter.api.AfterEach;

/**
 * 指令 Use Case 測試共用的場景。
 *
 * <p>標準看板 board-1「產品開發」：「待辦」(col-todo) 有 card-1；「進行中」(col-doing) 上限 1、有 card-2。
 */
public abstract class CommandFixture {

    protected static final Instant T0 = Instant.parse("2026-11-14T01:00:00Z");
    protected static final Instant NOW = Instant.parse("2026-11-14T02:00:00Z");
    protected static final BoardId BOARD = new BoardId("board-1");
    protected static final ColumnId TODO = new ColumnId("col-todo");
    protected static final ColumnId DOING = new ColumnId("col-doing");
    protected static final CardId CARD_1 = new CardId("card-1");
    protected static final CardId CARD_2 = new CardId("card-2");

    protected final FakeBoardRepository boards = new FakeBoardRepository();
    protected final TimeSource time = () -> NOW;
    protected final IdentityGenerator ids = new IdentityGenerator() {
        private int next = 1;

        @Override
        public String nextId() {
            return "id-" + next++;
        }
    };
    /**
     * Use Case 的回覆（測試把 responses::add 當 Output Boundary）。每個指令恰好回覆一次，
     * 所以收到回覆＝一個指令結束：在這裡自動檢查 DDD-5（這次指令最多只存了一個看板）。
     */
    protected final List<CommandResponse> responses = new ArrayList<>() {
        @Override
        public boolean add(CommandResponse response) {
            boards.commandFinished();
            return super.add(response);
        }
    };

    private int givenCount;

    /** DDD-5：測試結束時再檢查一次——沒有回覆就中斷的指令（例如程式錯誤）也不能存了兩個看板。 */
    @AfterEach
    protected void 一個指令只修改一個看板() {
        boards.assertAtMostOneBoardSaved();
    }

    /** 準備標準看板，後面可以再接額外的事件。 */
    protected void givenStandardBoard(BoardEvent... more) {
        List<BoardEvent> history = new ArrayList<>(List.of(
                new BoardCreated(BOARD, new BoardName("產品開發"), T0),
                new ColumnAdded(BOARD, TODO, new ColumnName("待辦"), T0),
                new ColumnAdded(BOARD, DOING, new ColumnName("進行中"), T0),
                new ColumnWipLimitSet(BOARD, DOING, new WipLimit(1), T0),
                new CardAdded(BOARD, CARD_1, TODO, new CardTitle("第一張"), new CardDescription(""), T0),
                new CardAdded(BOARD, CARD_2, DOING, new CardTitle("第二張"), new CardDescription(""), T0)));
        history.addAll(List.of(more));
        boards.given(BOARD, history);
        givenCount = history.size();
    }

    /** Use Case 必須恰好回覆一次。 */
    protected CommandResponse onlyResponse() {
        assertThat(responses).as("Use Case 必須恰好呼叫 Output Boundary 一次").hasSize(1);
        return responses.get(0);
    }

    /** 標準看板在這次指令之後多存了哪些事件。 */
    protected List<BoardEvent> newEvents() {
        List<BoardEvent> all = boards.eventsOf(BOARD);
        return all.subList(givenCount, all.size());
    }

    protected void assertSucceeded(String expectedCreatedId) {
        assertThat(onlyResponse()).isEqualTo(new CommandResponse(true, null, expectedCreatedId));
    }

    protected void assertFailed(String expectedCode) {
        assertThat(onlyResponse()).isEqualTo(new CommandResponse(false, expectedCode, null));
    }
}
