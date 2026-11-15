package kanban.boardmanagement.adapters.projection;

import static kanban.boardmanagement.adapters.projection.ProjectionEvents.BOARD;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.C1;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.C2;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.DOING;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.T0;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.T1;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.T2;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.TODO;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.card;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.column;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.created;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.moved;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import kanban.boardmanagement.entities.board.BoardArchived;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.entities.board.BoardRenamed;
import kanban.boardmanagement.entities.board.CardContentChanged;
import kanban.boardmanagement.entities.board.CardDescription;
import kanban.boardmanagement.entities.board.CardRemoved;
import kanban.boardmanagement.entities.board.CardRepositioned;
import kanban.boardmanagement.entities.board.CardTitle;
import kanban.boardmanagement.entities.board.ColumnMoved;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.entities.board.ColumnRemoved;
import kanban.boardmanagement.entities.board.ColumnRenamed;
import kanban.boardmanagement.entities.board.ColumnWipLimitRemoved;
import kanban.boardmanagement.entities.board.ColumnWipLimitSet;
import kanban.boardmanagement.entities.board.Position;
import kanban.boardmanagement.entities.board.WipLimit;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.CardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.ColumnSnapshot;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryEntry;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryEntryKind;
import org.junit.jupiter.api.Test;

/** 兩個純函式：把一筆事件抄進讀取模型。每種事件一支測試。 */
class ProjectorsTest {

    private static BoardSnapshot after(BoardEvent... events) {
        BoardSnapshot snapshot = null;
        for (BoardEvent event : events) {
            snapshot = BoardSnapshotProjector.apply(snapshot, event);
        }
        return snapshot;
    }

    private static List<String> columnNames(BoardSnapshot board) {
        return board.columns().stream().map(ColumnSnapshot::name).toList();
    }

    private static List<String> cardIds(BoardSnapshot board, int columnIndex) {
        return board.columns().get(columnIndex).cards().stream().map(CardSnapshot::cardId).toList();
    }

    // ---------------------------------------------------------------- 看板現況

    @Test
    void BoardCreated_產生沒有欄位的看板() {
        assertThat(after(created())).isEqualTo(new BoardSnapshot("board-1", "產品開發", false, List.of()));
    }

    @Test
    void BoardRenamed_改名稱() {
        assertThat(after(created(), new BoardRenamed(BOARD, new BoardName("新產品"), T1)).name()).isEqualTo("新產品");
    }

    @Test
    void BoardArchived_標成已封存() {
        assertThat(after(created(), new BoardArchived(BOARD, T1)).archived()).isTrue();
    }

    @Test
    void ColumnAdded_加在最右邊且沒有上限() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"), column(DOING, "進行中"));

        assertThat(board.columns()).containsExactly(
                new ColumnSnapshot("col-todo", "待辦", null, List.of()),
                new ColumnSnapshot("col-doing", "進行中", null, List.of()));
    }

    @Test
    void ColumnRenamed_改欄位名稱() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"),
                new ColumnRenamed(BOARD, TODO, new ColumnName("準備中"), T1));

        assertThat(columnNames(board)).containsExactly("準備中");
    }

    @Test
    void ColumnMoved_調整順序() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"), column(DOING, "進行中"),
                new ColumnMoved(BOARD, DOING, new Position(0), T1));

        assertThat(columnNames(board)).containsExactly("進行中", "待辦");
    }

    @Test
    void ColumnWipLimitSet與Removed_設定與取消上限() {
        BoardSnapshot limited = after(created(), column(TODO, "待辦"),
                new ColumnWipLimitSet(BOARD, TODO, new WipLimit(3), T1));
        BoardSnapshot unlimited = BoardSnapshotProjector.apply(limited, new ColumnWipLimitRemoved(BOARD, TODO, T2));

        assertThat(limited.columns().get(0).wipLimit()).isEqualTo(3);
        assertThat(unlimited.columns().get(0).wipLimit()).isNull();
    }

    @Test
    void ColumnRemoved_拿掉欄位() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"), column(DOING, "進行中"),
                new ColumnRemoved(BOARD, TODO, T1));

        assertThat(columnNames(board)).containsExactly("進行中");
    }

    @Test
    void CardAdded_加在該欄最下面() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"), card(C1, TODO, "第一張"), card(C2, TODO, "第二張"));

        assertThat(board.columns().get(0).cards()).containsExactly(
                new CardSnapshot("card-1", "第一張", ""), new CardSnapshot("card-2", "第二張", ""));
    }

    @Test
    void CardContentChanged_改標題與說明() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"), card(C1, TODO, "第一張"),
                new CardContentChanged(BOARD, C1, new CardTitle("新標題"), new CardDescription("新說明"), T1));

        assertThat(board.columns().get(0).cards()).containsExactly(new CardSnapshot("card-1", "新標題", "新說明"));
    }

    @Test
    void CardMoved_從來源欄拿掉並插進目標欄的指定位置() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"), column(DOING, "進行中"),
                card(C1, TODO, "第一張"), card(C2, DOING, "第二張"), moved(C1, TODO, DOING, 0, T1));

        assertThat(cardIds(board, 0)).isEmpty();
        assertThat(cardIds(board, 1)).containsExactly("card-1", "card-2");
    }

    @Test
    void CardRepositioned_欄內換位置() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"), card(C1, TODO, "第一張"), card(C2, TODO, "第二張"),
                new CardRepositioned(BOARD, C2, TODO, new Position(0), T1));

        assertThat(cardIds(board, 0)).containsExactly("card-2", "card-1");
    }

    @Test
    void CardRemoved_從欄內拿掉() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"), card(C1, TODO, "第一張"), card(C2, TODO, "第二張"),
                new CardRemoved(BOARD, C1, TODO, T1));

        assertThat(cardIds(board, 0)).containsExactly("card-2");
    }

    @Test
    void Projection不重新檢查業務規則_事件說了算() {
        // 事件已經是事實：即使欄內張數超過上限，讀取模型也照抄（DDD-9：Projection 內沒有業務判斷）
        BoardSnapshot board = after(created(), column(DOING, "進行中"),
                new ColumnWipLimitSet(BOARD, DOING, new WipLimit(1), T0),
                card(C1, DOING, "第一張"), card(C2, DOING, "第二張"));

        assertThat(cardIds(board, 0)).containsExactly("card-1", "card-2");
    }

    // ---------------------------------------------------------------- 卡片移動歷程

    private static CardHistory historyAfter(BoardEvent... events) {
        BoardSnapshot board = null;
        CardHistory history = null;
        for (BoardEvent event : events) {
            Optional<CardHistory> next = CardHistoryProjector.apply(history, board, event);
            if (next.isPresent()) {
                history = next.get();
            }
            board = BoardSnapshotProjector.apply(board, event);
        }
        return history;
    }

    @Test
    void 卡片歷程_新增後換欄再刪除_三筆且欄名正確() {
        CardHistory history = historyAfter(created(), column(TODO, "待辦"), column(DOING, "進行中"),
                card(C1, TODO, "設計登入頁"), moved(C1, TODO, DOING, 0, T1), new CardRemoved(BOARD, C1, DOING, T2));

        assertThat(history).isEqualTo(new CardHistory("card-1", "設計登入頁", List.of(
                new CardHistoryEntry(CardHistoryEntryKind.ADDED, null, "待辦", T0),
                new CardHistoryEntry(CardHistoryEntryKind.MOVED, "待辦", "進行中", T1),
                new CardHistoryEntry(CardHistoryEntryKind.REMOVED, "進行中", null, T2))));
    }

    @Test
    void 欄位改名之後_舊歷程仍顯示當時的名稱() {
        CardHistory history = historyAfter(created(), column(TODO, "待辦"), column(DOING, "進行中"),
                card(C1, TODO, "卡片"),
                new ColumnRenamed(BOARD, TODO, new ColumnName("準備中"), T1),
                moved(C1, TODO, DOING, 0, T2));

        assertThat(history.entries()).containsExactly(
                new CardHistoryEntry(CardHistoryEntryKind.ADDED, null, "待辦", T0),
                new CardHistoryEntry(CardHistoryEntryKind.MOVED, "準備中", "進行中", T2));
    }

    @Test
    void 欄內排序_不產生歷程() {
        BoardSnapshot board = after(created(), column(TODO, "待辦"), card(C1, TODO, "一"), card(C2, TODO, "二"));

        assertThat(CardHistoryProjector.apply(null, board, new CardRepositioned(BOARD, C2, TODO, new Position(0), T1)))
                .isEmpty();
    }

    @Test
    void 改卡片內容_歷程的標題跟著更新_但不多一筆() {
        CardHistory history = historyAfter(created(), column(TODO, "待辦"), card(C1, TODO, "舊標題"),
                new CardContentChanged(BOARD, C1, new CardTitle("新標題"), new CardDescription("說明"), T1));

        assertThat(history.title()).isEqualTo("新標題");
        assertThat(history.entries()).hasSize(1);
    }

    @Test
    void 與卡片無關的事件_不產生歷程() {
        assertThat(CardHistoryProjector.apply(null, after(created()), column(TODO, "待辦"))).isEmpty();
        assertThat(CardHistoryProjector.apply(null, null, created())).isEmpty();
    }
}
