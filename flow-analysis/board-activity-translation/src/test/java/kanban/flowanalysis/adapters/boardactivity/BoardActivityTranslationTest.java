package kanban.flowanalysis.adapters.boardactivity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind;
import kanban.flowanalysis.usecases.commands.BoardChange;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityRequest;
import org.junit.jupiter.api.Test;

/** 防腐層的翻譯：看板管理的用語 → 流程分析的用語（看板→看板流程、欄位→流程欄位、卡片→工作項目）。只翻譯，不判斷。 */
class BoardActivityTranslationTest {

    private static final Instant AT = Instant.parse("2026-11-16T01:00:00Z");

    private static BoardActivity activity(BoardActivityKind kind, String boardName, String columnId, String columnName,
            Integer columnPosition, String cardId, String cardTitle, String fromColumnId, String toColumnId) {
        return new BoardActivity(42, kind, "board-1", AT, boardName, columnId, columnName, columnPosition, cardId,
                cardTitle, fromColumnId, toColumnId);
    }

    private static RecordBoardActivityRequest request(BoardChange change, String boardName, String flowColumnId,
            String flowColumnName, Integer flowColumnPosition, String workItemId, String workItemTitle,
            String fromFlowColumnId, String toFlowColumnId) {
        return new RecordBoardActivityRequest(42L, change, "board-1", AT, boardName, flowColumnId, flowColumnName,
                flowColumnPosition, workItemId, workItemTitle, fromFlowColumnId, toFlowColumnId);
    }

    private static RecordBoardActivityRequest translate(BoardActivity activity) {
        return BoardActivityTranslation.toRequest(activity);
    }

    @Test
    void 看板建立_翻成看板流程建立() {
        assertThat(translate(activity(BoardActivityKind.BOARD_CREATED, "產品開發", null, null, null, null, null, null, null)))
                .isEqualTo(request(BoardChange.BOARD_FLOW_CREATED, "產品開發", null, null, null, null, null, null, null));
    }

    @Test
    void 看板改名_翻成看板流程改名() {
        assertThat(translate(activity(BoardActivityKind.BOARD_RENAMED, "新名稱", null, null, null, null, null, null, null)))
                .isEqualTo(request(BoardChange.BOARD_FLOW_RENAMED, "新名稱", null, null, null, null, null, null, null));
    }

    @Test
    void 看板封存_翻成看板流程封存() {
        assertThat(translate(activity(BoardActivityKind.BOARD_ARCHIVED, null, null, null, null, null, null, null, null)))
                .isEqualTo(request(BoardChange.BOARD_FLOW_ARCHIVED, null, null, null, null, null, null, null, null));
    }

    @Test
    void 欄位新增_翻成流程欄位新增() {
        assertThat(translate(activity(BoardActivityKind.COLUMN_ADDED, null, "col-todo", "待辦", null, null, null, null, null)))
                .isEqualTo(request(BoardChange.FLOW_COLUMN_ADDED, null, "col-todo", "待辦", null, null, null, null, null));
    }

    @Test
    void 欄位改名_翻成流程欄位改名() {
        assertThat(translate(activity(BoardActivityKind.COLUMN_RENAMED, null, "col-todo", "Todo", null, null, null, null, null)))
                .isEqualTo(request(BoardChange.FLOW_COLUMN_RENAMED, null, "col-todo", "Todo", null, null, null, null, null));
    }

    @Test
    void 欄位調整順序_翻成流程欄位調整順序() {
        assertThat(translate(activity(BoardActivityKind.COLUMN_MOVED, null, "col-todo", null, 2, null, null, null, null)))
                .isEqualTo(request(BoardChange.FLOW_COLUMN_MOVED, null, "col-todo", null, 2, null, null, null, null));
    }

    @Test
    void 欄位刪除_翻成流程欄位刪除() {
        assertThat(translate(activity(BoardActivityKind.COLUMN_REMOVED, null, "col-todo", null, null, null, null, null, null)))
                .isEqualTo(request(BoardChange.FLOW_COLUMN_REMOVED, null, "col-todo", null, null, null, null, null, null));
    }

    @Test
    void 卡片新增_翻成工作項目新增() {
        assertThat(translate(activity(BoardActivityKind.CARD_ADDED, null, "col-todo", null, null, "card-1", "串接金流", null, null)))
                .isEqualTo(request(BoardChange.WORK_ITEM_ADDED, null, "col-todo", null, null, "card-1", "串接金流", null, null));
    }

    @Test
    void 卡片內容修改_翻成工作項目標題修改() {
        assertThat(translate(activity(BoardActivityKind.CARD_CONTENT_CHANGED, null, null, null, null, "card-1", "改過", null, null)))
                .isEqualTo(request(BoardChange.WORK_ITEM_TITLE_CHANGED, null, null, null, null, "card-1", "改過", null, null));
    }

    @Test
    void 卡片換欄_翻成工作項目換欄() {
        assertThat(translate(activity(BoardActivityKind.CARD_MOVED, null, null, null, null, "card-1", null, "col-todo", "col-doing")))
                .isEqualTo(request(BoardChange.WORK_ITEM_MOVED, null, null, null, null, "card-1", null, "col-todo", "col-doing"));
    }

    @Test
    void 卡片刪除_翻成工作項目刪除() {
        assertThat(translate(activity(BoardActivityKind.CARD_REMOVED, null, "col-doing", null, null, "card-1", null, null, null)))
                .isEqualTo(request(BoardChange.WORK_ITEM_REMOVED, null, "col-doing", null, null, "card-1", null, null, null));
    }
}
