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

class CardRulesTest {

    private static final ColumnId TODO = new ColumnId("col-todo");
    private static final ColumnId DOING = new ColumnId("col-doing");
    private static final CardId C1 = new CardId("card-1");
    private static final CardId C2 = new CardId("card-2");
    private static final CardId C3 = new CardId("card-3");
    private static final CardId MISSING = new CardId("card-missing");
    private static final CardDescription NO_DESCRIPTION = new CardDescription("");

    private static final BoardCreated CREATED = new BoardCreated(BOARD, new BoardName("產品開發"), T0);
    private static final ColumnAdded TODO_ADDED = new ColumnAdded(BOARD, TODO, new ColumnName("待辦"), T0);
    private static final ColumnAdded DOING_ADDED = new ColumnAdded(BOARD, DOING, new ColumnName("進行中"), T0);

    private static CardAdded cardAdded(CardId card, ColumnId column, String title) {
        return new CardAdded(BOARD, card, column, new CardTitle(title), NO_DESCRIPTION, T0);
    }

    private static ColumnWipLimitSet limit(ColumnId column, int value) {
        return new ColumnWipLimitSet(BOARD, column, new WipLimit(value), T0);
    }

    // ------------------------------------------------------------ 新增卡片

    @Test
    void 新增卡片_產生CardAdded() {
        given(CREATED, TODO_ADDED)
                .when(board -> board.addCard(C1, TODO, new CardTitle("設計登入頁"), new CardDescription("含忘記密碼"), T1))
                .thenEvents(new CardAdded(BOARD, C1, TODO, new CardTitle("設計登入頁"), new CardDescription("含忘記密碼"), T1));
    }

    @Test
    void 新增卡片_加在該欄最下面() {
        // R9：第二張卡片已經在位置 1，再排到位置 1 不會有事件
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "第一張"), cardAdded(C2, TODO, "第二張"))
                .when(board -> board.repositionCard(C2, new Position(1), T1))
                .thenNoEvents();
    }

    @Test
    void 新增卡片到不存在的欄_拒絕COLUMN_NOT_FOUND() {
        given(CREATED, TODO_ADDED)
                .when(board -> board.addCard(C1, new ColumnId("col-missing"), new CardTitle("卡片"), NO_DESCRIPTION, T1))
                .thenRejected(RejectionReason.COLUMN_NOT_FOUND);
    }

    @Test
    void 新增卡片會超過上限_拒絕WIP_LIMIT_EXCEEDED() {
        given(CREATED, DOING_ADDED, limit(DOING, 1), cardAdded(C1, DOING, "已在進行"))
                .when(board -> board.addCard(C2, DOING, new CardTitle("再加一張"), NO_DESCRIPTION, T1))
                .thenRejected(RejectionReason.WIP_LIMIT_EXCEEDED);
    }

    @Test
    void 新增卡片後剛好等於上限_成功() {
        given(CREATED, DOING_ADDED, limit(DOING, 2), cardAdded(C1, DOING, "已在進行"))
                .when(board -> board.addCard(C2, DOING, new CardTitle("第二張"), NO_DESCRIPTION, T1))
                .thenEvents(new CardAdded(BOARD, C2, DOING, new CardTitle("第二張"), NO_DESCRIPTION, T1));
    }

    // ------------------------------------------------------------ 換欄

    @Test
    void 移動卡片_產生CardMoved並帶來源欄與目標欄() {
        given(CREATED, TODO_ADDED, DOING_ADDED, cardAdded(C1, TODO, "卡片"))
                .when(board -> board.moveCard(C1, DOING, new Position(0), T1))
                .thenEvents(new CardMoved(BOARD, C1, TODO, DOING, new Position(0), T1));
    }

    @Test
    void 移入卡片會超過上限_拒絕WIP_LIMIT_EXCEEDED() {
        given(CREATED, TODO_ADDED, DOING_ADDED, limit(DOING, 1),
                cardAdded(C1, DOING, "已在進行"), cardAdded(C2, TODO, "想移進去"))
                .when(board -> board.moveCard(C2, DOING, new Position(0), T1))
                .thenRejected(RejectionReason.WIP_LIMIT_EXCEEDED);
    }

    @Test
    void 移入後剛好等於上限_成功() {
        given(CREATED, TODO_ADDED, DOING_ADDED, limit(DOING, 2),
                cardAdded(C1, DOING, "已在進行"), cardAdded(C2, TODO, "想移進去"))
                .when(board -> board.moveCard(C2, DOING, new Position(1), T1))
                .thenEvents(new CardMoved(BOARD, C2, TODO, DOING, new Position(1), T1));
    }

    @Test
    void 卡片移走後_原本的欄空出一個名額() {
        given(CREATED, TODO_ADDED, DOING_ADDED, limit(DOING, 1),
                cardAdded(C1, DOING, "已在進行"), cardAdded(C2, TODO, "想移進去"),
                new CardMoved(BOARD, C1, DOING, TODO, new Position(0), T0))
                .when(board -> board.moveCard(C2, DOING, new Position(0), T1))
                .thenEvents(new CardMoved(BOARD, C2, TODO, DOING, new Position(0), T1));
    }

    @Test
    void 卡片換欄後_真的在目標欄的指定位置() {
        // C3 插進「進行中」的位置 1（C1 與 C2 之間）；之後把 C2 排到位置 2 不會有事件，代表 C2 被擠到了位置 2
        given(CREATED, TODO_ADDED, DOING_ADDED,
                cardAdded(C1, DOING, "一"), cardAdded(C2, DOING, "二"), cardAdded(C3, TODO, "三"),
                new CardMoved(BOARD, C3, TODO, DOING, new Position(1), T0))
                .when(board -> board.repositionCard(C2, new Position(2), T1))
                .thenNoEvents();
    }

    @Test
    void 移到不存在的欄_拒絕COLUMN_NOT_FOUND() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "卡片"))
                .when(board -> board.moveCard(C1, new ColumnId("col-missing"), new Position(0), T1))
                .thenRejected(RejectionReason.COLUMN_NOT_FOUND);
    }

    @Test
    void 換欄的目標位置超出範圍_拒絕POSITION_OUT_OF_RANGE() {
        // 目標欄有 1 張卡片，合法位置是 0 與 1
        given(CREATED, TODO_ADDED, DOING_ADDED, cardAdded(C1, TODO, "卡片"), cardAdded(C2, DOING, "已在進行"))
                .when(board -> board.moveCard(C1, DOING, new Position(2), T1))
                .thenRejected(RejectionReason.POSITION_OUT_OF_RANGE);
    }

    @Test
    void 換欄可以放到目標欄的最後面() {
        given(CREATED, TODO_ADDED, DOING_ADDED, cardAdded(C1, TODO, "卡片"), cardAdded(C2, DOING, "已在進行"))
                .when(board -> board.moveCard(C1, DOING, new Position(1), T1))
                .thenEvents(new CardMoved(BOARD, C1, TODO, DOING, new Position(1), T1));
    }

    @Test
    void 用移動卡片移到自己所在的欄_視為欄內排序() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "一"), cardAdded(C2, TODO, "二"))
                .when(board -> board.moveCard(C2, TODO, new Position(0), T1))
                .thenEvents(new CardRepositioned(BOARD, C2, TODO, new Position(0), T1));
    }

    // ------------------------------------------------------------ 欄內排序

    @Test
    void 欄內排序_產生CardRepositioned() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "一"), cardAdded(C2, TODO, "二"), cardAdded(C3, TODO, "三"))
                .when(board -> board.repositionCard(C3, new Position(0), T1))
                .thenEvents(new CardRepositioned(BOARD, C3, TODO, new Position(0), T1));
    }

    @Test
    void 已達上限的欄內仍可排序() {
        // R3：排序不改變張數，不受在製品上限影響
        given(CREATED, DOING_ADDED, cardAdded(C1, DOING, "一"), cardAdded(C2, DOING, "二"), limit(DOING, 2))
                .when(board -> board.repositionCard(C2, new Position(0), T1))
                .thenEvents(new CardRepositioned(BOARD, C2, DOING, new Position(0), T1));
    }

    @Test
    void 卡片排到原位置_不產生事件() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "一"), cardAdded(C2, TODO, "二"))
                .when(board -> board.repositionCard(C1, new Position(0), T1))
                .thenNoEvents();
    }

    @Test
    void 欄內排序的位置超出範圍_拒絕POSITION_OUT_OF_RANGE() {
        // 欄內有 2 張，合法位置是 0 與 1
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "一"), cardAdded(C2, TODO, "二"))
                .when(board -> board.repositionCard(C1, new Position(2), T1))
                .thenRejected(RejectionReason.POSITION_OUT_OF_RANGE);
    }

    // ------------------------------------------------------------ 在製品上限與欄位刪除

    @Test
    void 上限設得比現有張數低_拒絕WIP_LIMIT_BELOW_CARD_COUNT() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "一"), cardAdded(C2, TODO, "二"))
                .when(board -> board.setColumnWipLimit(TODO, new WipLimit(1), T1))
                .thenRejected(RejectionReason.WIP_LIMIT_BELOW_CARD_COUNT);
    }

    @Test
    void 上限等於現有張數_成功() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "一"), cardAdded(C2, TODO, "二"))
                .when(board -> board.setColumnWipLimit(TODO, new WipLimit(2), T1))
                .thenEvents(new ColumnWipLimitSet(BOARD, TODO, new WipLimit(2), T1));
    }

    @Test
    void 取消上限後_可以超過原本的上限() {
        given(CREATED, DOING_ADDED, limit(DOING, 1), cardAdded(C1, DOING, "一"),
                new ColumnWipLimitRemoved(BOARD, DOING, T0))
                .when(board -> board.addCard(C2, DOING, new CardTitle("二"), NO_DESCRIPTION, T1))
                .thenEvents(new CardAdded(BOARD, C2, DOING, new CardTitle("二"), NO_DESCRIPTION, T1));
    }

    @Test
    void 欄內有卡片不能刪欄_拒絕COLUMN_NOT_EMPTY() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "卡片"))
                .when(board -> board.removeColumn(TODO, T1))
                .thenRejected(RejectionReason.COLUMN_NOT_EMPTY);
    }

    @Test
    void 卡片移走後可以刪欄() {
        given(CREATED, TODO_ADDED, DOING_ADDED, cardAdded(C1, TODO, "卡片"),
                new CardMoved(BOARD, C1, TODO, DOING, new Position(0), T0))
                .when(board -> board.removeColumn(TODO, T1))
                .thenEvents(new ColumnRemoved(BOARD, TODO, T1));
    }

    @Test
    void 卡片刪除後可以刪欄() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "卡片"), new CardRemoved(BOARD, C1, TODO, T0))
                .when(board -> board.removeColumn(TODO, T1))
                .thenEvents(new ColumnRemoved(BOARD, TODO, T1));
    }

    // ------------------------------------------------------------ 改內容與刪除

    @Test
    void 改卡片內容_產生CardContentChanged() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "舊標題"))
                .when(board -> board.changeCardContent(C1, new CardTitle("新標題"), new CardDescription("補充說明"), T1))
                .thenEvents(new CardContentChanged(BOARD, C1, new CardTitle("新標題"), new CardDescription("補充說明"), T1));
    }

    @Test
    void 內容沒變_不產生事件() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "標題"))
                .when(board -> board.changeCardContent(C1, new CardTitle("標題"), NO_DESCRIPTION, T1))
                .thenNoEvents();
    }

    @Test
    void 內容改過之後再改成一樣的_不產生事件() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "舊標題"),
                new CardContentChanged(BOARD, C1, new CardTitle("新標題"), new CardDescription("說明"), T0))
                .when(board -> board.changeCardContent(C1, new CardTitle("新標題"), new CardDescription("說明"), T1))
                .thenNoEvents();
    }

    @Test
    void 刪除卡片_產生CardRemoved並帶所在欄() {
        given(CREATED, TODO_ADDED, DOING_ADDED, cardAdded(C1, TODO, "卡片"),
                new CardMoved(BOARD, C1, TODO, DOING, new Position(0), T0))
                .when(board -> board.removeCard(C1, T1))
                .thenEvents(new CardRemoved(BOARD, C1, DOING, T1));
    }

    @Test
    void 刪除後該欄張數減少_原本放不進的卡片現在放得進() {
        given(CREATED, DOING_ADDED, limit(DOING, 1), cardAdded(C1, DOING, "一"),
                new CardRemoved(BOARD, C1, DOING, T0))
                .when(board -> board.addCard(C2, DOING, new CardTitle("二"), NO_DESCRIPTION, T1))
                .thenEvents(new CardAdded(BOARD, C2, DOING, new CardTitle("二"), NO_DESCRIPTION, T1));
    }

    @Test
    void 卡片刪除後_不能再操作它() {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "卡片"), new CardRemoved(BOARD, C1, TODO, T0))
                .when(board -> board.removeCard(C1, T1))
                .thenRejected(RejectionReason.CARD_NOT_FOUND);
    }

    static Stream<Arguments> cardCommandsOnMissingCard() {
        return Stream.of(
                Arguments.of("改內容", (Consumer<Board>) b -> b.changeCardContent(MISSING, new CardTitle("標題"), NO_DESCRIPTION, T1)),
                Arguments.of("換欄", (Consumer<Board>) b -> b.moveCard(MISSING, TODO, new Position(0), T1)),
                Arguments.of("欄內排序", (Consumer<Board>) b -> b.repositionCard(MISSING, new Position(0), T1)),
                Arguments.of("刪除", (Consumer<Board>) b -> b.removeCard(MISSING, T1)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cardCommandsOnMissingCard")
    void 操作不存在的卡片_拒絕CARD_NOT_FOUND(String label, Consumer<Board> command) {
        given(CREATED, TODO_ADDED, cardAdded(C1, TODO, "卡片"))
                .when(command)
                .thenRejected(RejectionReason.CARD_NOT_FOUND);
    }

    static Stream<Arguments> cardCommands() {
        return Stream.of(
                Arguments.of("新增", (Consumer<Board>) b -> b.addCard(C2, TODO, new CardTitle("新卡"), NO_DESCRIPTION, T1)),
                Arguments.of("改內容", (Consumer<Board>) b -> b.changeCardContent(C1, new CardTitle("新標題"), NO_DESCRIPTION, T1)),
                Arguments.of("換欄", (Consumer<Board>) b -> b.moveCard(C1, DOING, new Position(0), T1)),
                Arguments.of("欄內排序", (Consumer<Board>) b -> b.repositionCard(C1, new Position(0), T1)),
                Arguments.of("刪除", (Consumer<Board>) b -> b.removeCard(C1, T1)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cardCommands")
    void 已封存的看板_拒絕所有卡片變更(String label, Consumer<Board> command) {
        given(CREATED, TODO_ADDED, DOING_ADDED, cardAdded(C1, TODO, "卡片"), new BoardArchived(BOARD, T0))
                .when(command)
                .thenRejected(RejectionReason.BOARD_ARCHIVED);
    }
}
