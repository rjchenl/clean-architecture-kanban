package kanban.boardmanagement.entities.board;

import static kanban.boardmanagement.entities.board.BoardScenario.BOARD;
import static kanban.boardmanagement.entities.board.BoardScenario.T0;
import static kanban.boardmanagement.entities.board.BoardScenario.T1;
import static kanban.boardmanagement.entities.board.BoardScenario.given;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ColumnRulesTest {

    private static final BoardCreated CREATED = new BoardCreated(BOARD, new BoardName("產品開發"), T0);
    private static final ColumnId TODO = new ColumnId("col-todo");
    private static final ColumnId DOING = new ColumnId("col-doing");
    private static final ColumnId DONE = new ColumnId("col-done");
    private static final ColumnId MISSING = new ColumnId("col-missing");
    private static final ColumnAdded TODO_ADDED = new ColumnAdded(BOARD, TODO, new ColumnName("待辦"), T0);
    private static final ColumnAdded DOING_ADDED = new ColumnAdded(BOARD, DOING, new ColumnName("進行中"), T0);
    private static final ColumnAdded DONE_ADDED = new ColumnAdded(BOARD, DONE, new ColumnName("完成"), T0);

    @Test
    void 新增欄位_產生ColumnAdded() {
        given(CREATED)
                .when(board -> board.addColumn(TODO, new ColumnName("待辦"), T1))
                .thenEvents(new ColumnAdded(BOARD, TODO, new ColumnName("待辦"), T1));
    }

    @Test
    void 新增欄位_加在最右邊() {
        // R9：「進行中」是第二個加的，所以它已經在位置 1；再把它移到位置 1 不會有事件
        given(CREATED, TODO_ADDED, DOING_ADDED)
                .when(board -> board.moveColumn(DOING, new Position(1), T1))
                .thenNoEvents();
    }

    @Test
    void 欄位名稱重複_拒絕COLUMN_NAME_DUPLICATED() {
        given(CREATED, TODO_ADDED)
                .when(board -> board.addColumn(DOING, new ColumnName(" 待辦 "), T1))
                .thenRejected(RejectionReason.COLUMN_NAME_DUPLICATED);
    }

    @Test
    void 欄位改名_產生ColumnRenamed() {
        given(CREATED, TODO_ADDED)
                .when(board -> board.renameColumn(TODO, new ColumnName("準備中"), T1))
                .thenEvents(new ColumnRenamed(BOARD, TODO, new ColumnName("準備中"), T1));
    }

    @Test
    void 欄位改成自己原本的名稱_不產生事件() {
        given(CREATED, TODO_ADDED)
                .when(board -> board.renameColumn(TODO, new ColumnName("待辦"), T1))
                .thenNoEvents();
    }

    @Test
    void 欄位改成別欄已用的名稱_拒絕() {
        given(CREATED, TODO_ADDED, DOING_ADDED)
                .when(board -> board.renameColumn(DOING, new ColumnName("待辦"), T1))
                .thenRejected(RejectionReason.COLUMN_NAME_DUPLICATED);
    }

    @Test
    void 欄位改名後_舊名稱可以給新欄位用() {
        given(CREATED, TODO_ADDED, new ColumnRenamed(BOARD, TODO, new ColumnName("準備中"), T0))
                .when(board -> board.addColumn(DOING, new ColumnName("待辦"), T1))
                .thenEvents(new ColumnAdded(BOARD, DOING, new ColumnName("待辦"), T1));
    }

    @Test
    void 調整欄位順序_產生ColumnMoved() {
        given(CREATED, TODO_ADDED, DOING_ADDED, DONE_ADDED)
                .when(board -> board.moveColumn(DONE, new Position(0), T1))
                .thenEvents(new ColumnMoved(BOARD, DONE, new Position(0), T1));
    }

    @Test
    void 調整欄位順序後_欄位真的換了位置() {
        // 「完成」移到最左邊後，「待辦」變成位置 1
        given(CREATED, TODO_ADDED, DOING_ADDED, DONE_ADDED, new ColumnMoved(BOARD, DONE, new Position(0), T0))
                .when(board -> board.moveColumn(TODO, new Position(1), T1))
                .thenNoEvents();
    }

    @Test
    void 欄位移到原位置_不產生事件() {
        given(CREATED, TODO_ADDED, DOING_ADDED)
                .when(board -> board.moveColumn(TODO, new Position(0), T1))
                .thenNoEvents();
    }

    @Test
    void 欄位位置超出範圍_拒絕POSITION_OUT_OF_RANGE() {
        given(CREATED, TODO_ADDED, DOING_ADDED, DONE_ADDED)
                .when(board -> board.moveColumn(TODO, new Position(3), T1))
                .thenRejected(RejectionReason.POSITION_OUT_OF_RANGE);
    }

    @Test
    void 欄位可以移到最後一個位置() {
        given(CREATED, TODO_ADDED, DOING_ADDED, DONE_ADDED)
                .when(board -> board.moveColumn(TODO, new Position(2), T1))
                .thenEvents(new ColumnMoved(BOARD, TODO, new Position(2), T1));
    }

    @Test
    void 設定在製品上限_產生ColumnWipLimitSet() {
        given(CREATED, TODO_ADDED)
                .when(board -> board.setColumnWipLimit(TODO, new WipLimit(3), T1))
                .thenEvents(new ColumnWipLimitSet(BOARD, TODO, new WipLimit(3), T1));
    }

    @Test
    void 設成相同上限_不產生事件() {
        given(CREATED, TODO_ADDED, new ColumnWipLimitSet(BOARD, TODO, new WipLimit(3), T0))
                .when(board -> board.setColumnWipLimit(TODO, new WipLimit(3), T1))
                .thenNoEvents();
    }

    @Test
    void 改成不同上限_產生事件() {
        given(CREATED, TODO_ADDED, new ColumnWipLimitSet(BOARD, TODO, new WipLimit(3), T0))
                .when(board -> board.setColumnWipLimit(TODO, new WipLimit(5), T1))
                .thenEvents(new ColumnWipLimitSet(BOARD, TODO, new WipLimit(5), T1));
    }

    @Test
    void 取消上限_產生ColumnWipLimitRemoved() {
        given(CREATED, TODO_ADDED, new ColumnWipLimitSet(BOARD, TODO, new WipLimit(3), T0))
                .when(board -> board.removeColumnWipLimit(TODO, T1))
                .thenEvents(new ColumnWipLimitRemoved(BOARD, TODO, T1));
    }

    @Test
    void 本來就沒上限又取消_不產生事件() {
        given(CREATED, TODO_ADDED)
                .when(board -> board.removeColumnWipLimit(TODO, T1))
                .thenNoEvents();
    }

    @Test
    void 取消上限之後再取消_不產生事件() {
        given(CREATED, TODO_ADDED,
                new ColumnWipLimitSet(BOARD, TODO, new WipLimit(3), T0),
                new ColumnWipLimitRemoved(BOARD, TODO, T0))
                .when(board -> board.removeColumnWipLimit(TODO, T1))
                .thenNoEvents();
    }

    @Test
    void 刪除空欄位_產生ColumnRemoved() {
        given(CREATED, TODO_ADDED)
                .when(board -> board.removeColumn(TODO, T1))
                .thenEvents(new ColumnRemoved(BOARD, TODO, T1));
    }

    @Test
    void 欄位刪除後_不能再操作它() {
        given(CREATED, TODO_ADDED, new ColumnRemoved(BOARD, TODO, T0))
                .when(board -> board.renameColumn(TODO, new ColumnName("新名"), T1))
                .thenRejected(RejectionReason.COLUMN_NOT_FOUND);
    }

    @Test
    void 欄位刪除後_它的名稱可以再用() {
        given(CREATED, TODO_ADDED, new ColumnRemoved(BOARD, TODO, T0))
                .when(board -> board.addColumn(DOING, new ColumnName("待辦"), T1))
                .thenEvents(new ColumnAdded(BOARD, DOING, new ColumnName("待辦"), T1));
    }

    static Stream<Arguments> columnCommands() {
        return Stream.of(
                Arguments.of("欄位改名", (Consumer<Board>) b -> b.renameColumn(MISSING, new ColumnName("新名"), T1)),
                Arguments.of("調整欄位順序", (Consumer<Board>) b -> b.moveColumn(MISSING, new Position(0), T1)),
                Arguments.of("設定在製品上限", (Consumer<Board>) b -> b.setColumnWipLimit(MISSING, new WipLimit(3), T1)),
                Arguments.of("取消在製品上限", (Consumer<Board>) b -> b.removeColumnWipLimit(MISSING, T1)),
                Arguments.of("刪除欄位", (Consumer<Board>) b -> b.removeColumn(MISSING, T1)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("columnCommands")
    void 操作不存在的欄位_拒絕COLUMN_NOT_FOUND(String label, Consumer<Board> command) {
        given(CREATED, TODO_ADDED)
                .when(command)
                .thenRejected(RejectionReason.COLUMN_NOT_FOUND);
    }
}
