package kanban.boardmanagement.entities.board;

import static kanban.boardmanagement.entities.board.BoardScenario.BOARD;
import static kanban.boardmanagement.entities.board.BoardScenario.T0;
import static kanban.boardmanagement.entities.board.BoardScenario.T1;
import static kanban.boardmanagement.entities.board.BoardScenario.given;
import static kanban.boardmanagement.entities.board.BoardScenario.whenCreating;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BoardLifecycleTest {

    private static final BoardCreated CREATED = new BoardCreated(BOARD, new BoardName("產品開發"), T0);
    private static final ColumnId TODO = new ColumnId("col-todo");
    private static final ColumnAdded TODO_ADDED = new ColumnAdded(BOARD, TODO, new ColumnName("待辦"), T0);

    @Test
    void 建立看板_產生BoardCreated() {
        whenCreating(() -> Board.create(BOARD, new BoardName("產品開發"), T0))
                .thenEvents(CREATED);
    }

    @Test
    void 改名_產生BoardRenamed() {
        given(CREATED)
                .when(board -> board.rename(new BoardName("新產品"), T1))
                .thenEvents(new BoardRenamed(BOARD, new BoardName("新產品"), T1));
    }

    @Test
    void 改成相同名稱_不產生事件() {
        given(CREATED)
                .when(board -> board.rename(new BoardName(" 產品開發 "), T1))
                .thenNoEvents();
    }

    @Test
    void 改名之後再改回原名_產生事件() {
        given(CREATED, new BoardRenamed(BOARD, new BoardName("新產品"), T0))
                .when(board -> board.rename(new BoardName("產品開發"), T1))
                .thenEvents(new BoardRenamed(BOARD, new BoardName("產品開發"), T1));
    }

    @Test
    void 封存_產生BoardArchived() {
        given(CREATED)
                .when(board -> board.archive(T1))
                .thenEvents(new BoardArchived(BOARD, T1));
    }

    @Test
    void 已封存再封存_拒絕BOARD_ARCHIVED() {
        given(CREATED, new BoardArchived(BOARD, T0))
                .when(board -> board.archive(T1))
                .thenRejected(RejectionReason.BOARD_ARCHIVED);
    }

    static Stream<Arguments> boardAndColumnCommands() {
        return Stream.of(
                Arguments.of("改名", (Consumer<Board>) b -> b.rename(new BoardName("別的名字"), T1)),
                Arguments.of("封存", (Consumer<Board>) b -> b.archive(T1)),
                Arguments.of("新增欄位", (Consumer<Board>) b -> b.addColumn(new ColumnId("col-x"), new ColumnName("新欄"), T1)),
                Arguments.of("欄位改名", (Consumer<Board>) b -> b.renameColumn(TODO, new ColumnName("新名"), T1)),
                Arguments.of("調整欄位順序", (Consumer<Board>) b -> b.moveColumn(TODO, new Position(0), T1)),
                Arguments.of("設定在製品上限", (Consumer<Board>) b -> b.setColumnWipLimit(TODO, new WipLimit(3), T1)),
                Arguments.of("取消在製品上限", (Consumer<Board>) b -> b.removeColumnWipLimit(TODO, T1)),
                Arguments.of("刪除欄位", (Consumer<Board>) b -> b.removeColumn(TODO, T1)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("boardAndColumnCommands")
    void 已封存的看板_拒絕所有變更(String label, Consumer<Board> command) {
        given(CREATED, TODO_ADDED, new BoardArchived(BOARD, T0))
                .when(command)
                .thenRejected(RejectionReason.BOARD_ARCHIVED);
    }
}
