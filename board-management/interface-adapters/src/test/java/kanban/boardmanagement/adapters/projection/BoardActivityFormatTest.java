package kanban.boardmanagement.adapters.projection;

import static kanban.boardmanagement.adapters.projection.ProjectionEvents.BOARD;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.C1;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.DOING;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.T0;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.T1;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.TODO;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import kanban.boardmanagement.entities.board.BoardArchived;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.entities.board.BoardRenamed;
import kanban.boardmanagement.entities.board.CardAdded;
import kanban.boardmanagement.entities.board.CardContentChanged;
import kanban.boardmanagement.entities.board.CardDescription;
import kanban.boardmanagement.entities.board.CardMoved;
import kanban.boardmanagement.entities.board.CardRemoved;
import kanban.boardmanagement.entities.board.CardRepositioned;
import kanban.boardmanagement.entities.board.CardTitle;
import kanban.boardmanagement.entities.board.ColumnAdded;
import kanban.boardmanagement.entities.board.ColumnMoved;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.entities.board.ColumnRemoved;
import kanban.boardmanagement.entities.board.ColumnRenamed;
import kanban.boardmanagement.entities.board.ColumnWipLimitRemoved;
import kanban.boardmanagement.entities.board.ColumnWipLimitSet;
import kanban.boardmanagement.entities.board.Position;
import kanban.boardmanagement.entities.board.WipLimit;
import kanban.boardmanagement.usecases.eventfeed.RecordedEvent;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind;
import org.junit.jupiter.api.Test;

/** 看板動態的格式轉換：只是把看板管理的事件換成公布格式，沒有業務判斷（設計第 4.3 節）。 */
class BoardActivityFormatTest {

    private static final long POSITION = 42;

    private static Optional<BoardActivity> format(BoardEvent event) {
        return BoardActivityFormat.of(new RecordedEvent(POSITION, event));
    }

    private static BoardActivity activity(BoardActivityKind kind, java.time.Instant at, String boardName,
            String columnId, String columnName, Integer columnPosition, String cardId, String cardTitle,
            String fromColumnId, String toColumnId) {
        return new BoardActivity(POSITION, kind, "board-1", at, boardName, columnId, columnName, columnPosition,
                cardId, cardTitle, fromColumnId, toColumnId);
    }

    @Test
    void 看板建立_帶看板名稱() {
        assertThat(format(new BoardCreated(BOARD, new BoardName("產品開發"), T0))).contains(
                activity(BoardActivityKind.BOARD_CREATED, T0, "產品開發", null, null, null, null, null, null, null));
    }

    @Test
    void 看板改名_帶新名稱() {
        assertThat(format(new BoardRenamed(BOARD, new BoardName("新名稱"), T1))).contains(
                activity(BoardActivityKind.BOARD_RENAMED, T1, "新名稱", null, null, null, null, null, null, null));
    }

    @Test
    void 看板封存() {
        assertThat(format(new BoardArchived(BOARD, T1))).contains(
                activity(BoardActivityKind.BOARD_ARCHIVED, T1, null, null, null, null, null, null, null, null));
    }

    @Test
    void 欄位新增_帶欄位與名稱() {
        assertThat(format(new ColumnAdded(BOARD, TODO, new ColumnName("待辦"), T0))).contains(
                activity(BoardActivityKind.COLUMN_ADDED, T0, null, "col-todo", "待辦", null, null, null, null, null));
    }

    @Test
    void 欄位改名_帶欄位與新名稱() {
        assertThat(format(new ColumnRenamed(BOARD, TODO, new ColumnName("Todo"), T1))).contains(
                activity(BoardActivityKind.COLUMN_RENAMED, T1, null, "col-todo", "Todo", null, null, null, null, null));
    }

    @Test
    void 欄位調整順序_帶新位置() {
        assertThat(format(new ColumnMoved(BOARD, TODO, new Position(1), T1))).contains(
                activity(BoardActivityKind.COLUMN_MOVED, T1, null, "col-todo", null, 1, null, null, null, null));
    }

    @Test
    void 欄位刪除_帶欄位() {
        assertThat(format(new ColumnRemoved(BOARD, TODO, T1))).contains(
                activity(BoardActivityKind.COLUMN_REMOVED, T1, null, "col-todo", null, null, null, null, null, null));
    }

    @Test
    void 卡片新增_帶卡片所在欄與標題_不帶說明() {
        assertThat(format(new CardAdded(BOARD, C1, TODO, new CardTitle("第一張"), new CardDescription("說明"), T0)))
                .contains(activity(BoardActivityKind.CARD_ADDED, T0, null, "col-todo", null, null,
                        "card-1", "第一張", null, null));
    }

    @Test
    void 卡片內容修改_只帶標題() {
        assertThat(format(new CardContentChanged(BOARD, C1, new CardTitle("改過"), new CardDescription("新說明"), T1)))
                .contains(activity(BoardActivityKind.CARD_CONTENT_CHANGED, T1, null, null, null, null,
                        "card-1", "改過", null, null));
    }

    @Test
    void 卡片換欄_帶來源欄與目標欄() {
        assertThat(format(new CardMoved(BOARD, C1, TODO, DOING, new Position(0), T1))).contains(
                activity(BoardActivityKind.CARD_MOVED, T1, null, null, null, null, "card-1", null,
                        "col-todo", "col-doing"));
    }

    @Test
    void 卡片刪除_帶卡片與所在欄() {
        assertThat(format(new CardRemoved(BOARD, C1, DOING, T1))).contains(
                activity(BoardActivityKind.CARD_REMOVED, T1, null, "col-doing", null, null, "card-1", null, null, null));
    }

    @Test
    void 欄內排序_不公布() {
        assertThat(format(new CardRepositioned(BOARD, C1, TODO, new Position(0), T1))).isEmpty();
    }

    @Test
    void 設定在製品上限_不公布() {
        assertThat(format(new ColumnWipLimitSet(BOARD, TODO, new WipLimit(3), T1))).isEmpty();
    }

    @Test
    void 取消在製品上限_不公布() {
        assertThat(format(new ColumnWipLimitRemoved(BOARD, TODO, T1))).isEmpty();
    }
}
