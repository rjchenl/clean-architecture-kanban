package kanban.boardmanagement.entities.board;

import static kanban.boardmanagement.entities.board.BoardScenario.BOARD;
import static kanban.boardmanagement.entities.board.BoardScenario.T0;
import static kanban.boardmanagement.entities.board.BoardScenario.T1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** ES-4：載入聚合＝重播事件；同一串事件重播多次，結果必須相同。 */
class ReplayTest {

    private static final ColumnId TODO = new ColumnId("col-todo");
    private static final ColumnId DOING = new ColumnId("col-doing");
    private static final ColumnId DONE = new ColumnId("col-done");
    private static final CardId C1 = new CardId("card-1");
    private static final CardId C2 = new CardId("card-2");
    private static final CardDescription NONE = new CardDescription("");

    /** 每一種事件至少出現一次的歷史。結束時：「進行中」上限 1 且有 C1；「待辦」有 C2。 */
    private static final List<BoardEvent> HISTORY = List.of(
            new BoardCreated(BOARD, new BoardName("產品開發"), T0),
            new BoardRenamed(BOARD, new BoardName("新產品"), T0),
            new ColumnAdded(BOARD, TODO, new ColumnName("待辦"), T0),
            new ColumnAdded(BOARD, DOING, new ColumnName("進行"), T0),
            new ColumnAdded(BOARD, DONE, new ColumnName("完成"), T0),
            new ColumnRenamed(BOARD, DOING, new ColumnName("進行中"), T0),
            new ColumnMoved(BOARD, DONE, new Position(0), T0),
            new ColumnWipLimitSet(BOARD, TODO, new WipLimit(5), T0),
            new ColumnWipLimitRemoved(BOARD, TODO, T0),
            new ColumnRemoved(BOARD, DONE, T0),
            new CardAdded(BOARD, C1, TODO, new CardTitle("第一張"), NONE, T0),
            new CardAdded(BOARD, C2, TODO, new CardTitle("第二張"), NONE, T0),
            new CardContentChanged(BOARD, C1, new CardTitle("第一張（改）"), new CardDescription("說明"), T0),
            new CardRepositioned(BOARD, C2, TODO, new Position(0), T0),
            new CardMoved(BOARD, C1, TODO, DOING, new Position(0), T0),
            new ColumnWipLimitSet(BOARD, DOING, new WipLimit(1), T0),
            new CardAdded(BOARD, new CardId("card-3"), TODO, new CardTitle("第三張"), NONE, T0),
            new CardRemoved(BOARD, new CardId("card-3"), TODO, T0));

    @Test
    void 歷史涵蓋封存以外的每一種事件() {
        // 封存之後所有指令都會被拒絕，放不進這段歷史；封存的重播由下面另一支測試負責
        assertThat(HISTORY.stream().map(Object::getClass).collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(
                        Arrays.stream(BoardEvent.class.getPermittedSubclasses())
                                .filter(type -> type != BoardArchived.class)
                                .collect(Collectors.toSet()));
    }

    @Test
    void 封存過的歷史重播兩次_兩次都拒絕變更() {
        List<BoardEvent> archivedHistory = new java.util.ArrayList<>(HISTORY);
        archivedHistory.add(new BoardArchived(BOARD, T0));

        for (Board board : List.of(Board.rehydrate(archivedHistory), Board.rehydrate(archivedHistory))) {
            assertThatThrownBy(() -> board.moveCard(C1, TODO, new Position(0), T1))
                    .isInstanceOfSatisfying(BusinessRuleViolation.class,
                            v -> assertThat(v.reason()).isEqualTo(RejectionReason.BOARD_ARCHIVED));
            assertThat(board.uncommittedEvents()).isEmpty();
        }
    }

    @Test
    void 同一串事件重播兩次_後續指令的結果相同() {
        Board first = Board.rehydrate(HISTORY);
        Board second = Board.rehydrate(HISTORY);

        for (Board board : List.of(first, second)) {
            // 會被拒絕：「進行中」已達上限
            assertThatThrownBy(() -> board.moveCard(C2, DOING, new Position(0), T1))
                    .isInstanceOfSatisfying(BusinessRuleViolation.class,
                            v -> assertThat(v.reason()).isEqualTo(RejectionReason.WIP_LIMIT_EXCEEDED));
            // 會成功
            board.moveCard(C1, TODO, new Position(1), T1);
            board.removeColumn(DOING, T1);
        }

        assertThat(first.uncommittedEvents())
                .containsExactly(
                        new CardMoved(BOARD, C1, DOING, TODO, new Position(1), T1),
                        new ColumnRemoved(BOARD, DOING, T1))
                .isEqualTo(second.uncommittedEvents());
    }

    @Test
    void 重播不產生未提交事件_版本等於事件數() {
        Board board = Board.rehydrate(HISTORY);

        assertThat(board.uncommittedEvents()).isEmpty();
        assertThat(board.persistedVersion()).isEqualTo(HISTORY.size());
        assertThat(board.id()).isEqualTo(BOARD);
    }

    @Test
    void 新事件接在歷史後面再重播_與原聚合的行為相同() {
        Board original = Board.rehydrate(HISTORY);
        original.moveCard(C1, TODO, new Position(0), T1);
        List<BoardEvent> extended = new java.util.ArrayList<>(HISTORY);
        extended.addAll(original.uncommittedEvents());

        Board reloaded = Board.rehydrate(extended);
        original.repositionCard(C2, new Position(0), T1);
        reloaded.repositionCard(C2, new Position(0), T1);

        // original 另外多了前一步的 CardMoved，所以只比最後一筆
        assertThat(reloaded.uncommittedEvents())
                .containsExactly(new CardRepositioned(BOARD, C2, TODO, new Position(0), T1));
        assertThat(original.uncommittedEvents()).last()
                .isEqualTo(new CardRepositioned(BOARD, C2, TODO, new Position(0), T1));
    }

    @Test
    void 建立後存檔_版本前進且未提交事件清空() {
        Board board = Board.create(BOARD, new BoardName("產品開發"), T0);
        assertThat(board.persistedVersion()).isZero();

        board.addColumn(TODO, new ColumnName("待辦"), T0);
        board.markCommitted();

        assertThat(board.persistedVersion()).isEqualTo(2);
        assertThat(board.uncommittedEvents()).isEmpty();
    }

    @Test
    void 空的歷史不能重播() {
        assertThatThrownBy(() -> Board.rehydrate(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
