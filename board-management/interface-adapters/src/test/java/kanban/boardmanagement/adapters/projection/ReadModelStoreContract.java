package kanban.boardmanagement.adapters.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import kanban.boardmanagement.usecases.queries.listboards.BoardSummary;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.CardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.ColumnSnapshot;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryEntry;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryEntryKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 讀取模型儲存的契約（CA-6）：記憶體版與 PostgreSQL 版都要通過同一套測試。 */
public abstract class ReadModelStoreContract {

    protected static final Instant AT = Instant.parse("2026-11-14T01:00:00Z");

    protected abstract ReadModelStore store();

    protected static BoardSnapshot board(String id, String name) {
        return new BoardSnapshot(id, name, false, List.of(
                new ColumnSnapshot("col-todo", "待辦", null, List.of(new CardSnapshot("card-1", "第一張", "說明"))),
                new ColumnSnapshot("col-doing", "進行中", 2, List.of())));
    }

    protected static CardHistory history(String cardId, String title) {
        return new CardHistory(cardId, title, List.of(
                new CardHistoryEntry(CardHistoryEntryKind.ADDED, null, "待辦", AT),
                new CardHistoryEntry(CardHistoryEntryKind.MOVED, "待辦", "進行中", AT.plusSeconds(60)),
                new CardHistoryEntry(CardHistoryEntryKind.REMOVED, "進行中", null, AT.plusSeconds(120))));
    }

    @BeforeEach
    void startEmpty() {
        store().inExclusiveTransaction(() -> store().reset());
    }

    @Test
    void 一開始是空的_checkpoint為0() {
        assertThat(store().checkpoint()).isZero();
        assertThat(store().allBoards()).isEmpty();
        assertThat(store().boardById("board-1")).isEmpty();
        assertThat(store().cardHistory("board-1", "card-1")).isEmpty();
    }

    @Test
    void 存入的看板讀得回來() {
        store().inExclusiveTransaction(() -> store().saveBoard(board("board-1", "產品開發")));

        assertThat(store().boardById("board-1")).contains(board("board-1", "產品開發"));
        assertThat(store().boardById("board-2")).isEmpty();
    }

    @Test
    void 同一個看板再存一次_覆蓋舊的() {
        store().inExclusiveTransaction(() -> store().saveBoard(board("board-1", "產品開發")));
        BoardSnapshot archived = new BoardSnapshot("board-1", "新名稱", true, List.of());

        store().inExclusiveTransaction(() -> store().saveBoard(archived));

        assertThat(store().boardById("board-1")).contains(archived);
        assertThat(store().allBoards()).containsExactly(new BoardSummary("board-1", "新名稱", true));
    }

    @Test
    void allBoards依建立順序_更新不改變順序() {
        store().inExclusiveTransaction(() -> {
            store().saveBoard(board("board-b", "先建的"));
            store().saveBoard(board("board-a", "後建的"));
            store().saveBoard(board("board-b", "先建的（改名）"));
        });

        assertThat(store().allBoards()).containsExactly(
                new BoardSummary("board-b", "先建的（改名）", false),
                new BoardSummary("board-a", "後建的", false));
    }

    @Test
    void 存入的卡片歷程讀得回來() {
        store().inExclusiveTransaction(() -> {
            store().saveCardHistory("board-1", history("card-1", "第一張"));
            store().saveCardHistory("board-2", history("card-1", "別的看板的卡片"));
        });

        assertThat(store().cardHistory("board-1", "card-1")).contains(history("card-1", "第一張"));
        assertThat(store().cardHistory("board-2", "card-1")).contains(history("card-1", "別的看板的卡片"));
        assertThat(store().cardHistory("board-1", "card-2")).isEmpty();
    }

    @Test
    void checkpoint可以前進() {
        store().inExclusiveTransaction(() -> store().advanceCheckpoint(7));

        assertThat(store().checkpoint()).isEqualTo(7);
    }

    @Test
    void 交易內讀得到自己剛寫的資料() {
        store().inExclusiveTransaction(() -> {
            store().saveBoard(board("board-1", "產品開發"));
            store().advanceCheckpoint(3);

            assertThat(store().boardById("board-1")).isPresent();
            assertThat(store().checkpoint()).isEqualTo(3);
        });
    }

    @Test
    void reset後全部清空且checkpoint為0() {
        store().inExclusiveTransaction(() -> {
            store().saveBoard(board("board-1", "產品開發"));
            store().saveCardHistory("board-1", history("card-1", "第一張"));
            store().advanceCheckpoint(5);
        });

        store().inExclusiveTransaction(() -> store().reset());

        assertThat(store().checkpoint()).isZero();
        assertThat(store().allBoards()).isEmpty();
        assertThat(store().boardById("board-1")).isEmpty();
        assertThat(store().cardHistory("board-1", "card-1")).isEmpty();
    }

    @Test
    void inExclusiveTransaction內丟例外_寫入與checkpoint都不保留() {
        store().inExclusiveTransaction(() -> {
            store().saveBoard(board("board-1", "原本的"));
            store().advanceCheckpoint(1);
        });

        assertThatThrownBy(() -> store().inExclusiveTransaction(() -> {
            store().saveBoard(board("board-1", "不該留下"));
            store().saveBoard(board("board-2", "也不該留下"));
            store().saveCardHistory("board-1", history("card-9", "不該留下"));
            store().advanceCheckpoint(2);
            throw new IllegalStateException("投影失敗");
        })).isInstanceOf(IllegalStateException.class).hasMessage("投影失敗");

        assertThat(store().boardById("board-1")).contains(board("board-1", "原本的"));
        assertThat(store().boardById("board-2")).isEmpty();
        assertThat(store().cardHistory("board-1", "card-9")).isEmpty();
        assertThat(store().checkpoint()).isEqualTo(1);
    }

    @Test
    void 寫入方法只能在交易內呼叫() {
        assertThatThrownBy(() -> store().saveBoard(board("board-1", "產品開發"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store().advanceCheckpoint(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store().reset()).isInstanceOf(IllegalStateException.class);
    }
}
