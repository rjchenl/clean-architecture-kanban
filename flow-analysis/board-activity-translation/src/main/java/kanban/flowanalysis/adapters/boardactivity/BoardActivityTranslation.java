package kanban.flowanalysis.adapters.boardactivity;

import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.flowanalysis.usecases.commands.BoardChange;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityRequest;

/**
 * 防腐層的翻譯（DDD-3）：看板管理公布的看板動態 → 流程分析「記錄看板動態」指令的 Request Model。
 * 看板→看板流程、欄位→流程欄位、卡片→工作項目。只翻譯欄位與用語，沒有任何業務判斷。
 *
 * <p>對看板動態的種類逐一列出、不寫 default：看板管理新增一種動態時這裡會編譯失敗，
 * 逼人明確決定翻譯或略過（設計第 4.4 節）。
 */
public final class BoardActivityTranslation {

    private BoardActivityTranslation() {
    }

    public static RecordBoardActivityRequest toRequest(BoardActivity activity) {
        BoardChange change = switch (activity.kind()) {
            case BOARD_CREATED -> BoardChange.BOARD_FLOW_CREATED;
            case BOARD_RENAMED -> BoardChange.BOARD_FLOW_RENAMED;
            case BOARD_ARCHIVED -> BoardChange.BOARD_FLOW_ARCHIVED;
            case COLUMN_ADDED -> BoardChange.FLOW_COLUMN_ADDED;
            case COLUMN_RENAMED -> BoardChange.FLOW_COLUMN_RENAMED;
            case COLUMN_MOVED -> BoardChange.FLOW_COLUMN_MOVED;
            case COLUMN_REMOVED -> BoardChange.FLOW_COLUMN_REMOVED;
            case CARD_ADDED -> BoardChange.WORK_ITEM_ADDED;
            case CARD_CONTENT_CHANGED -> BoardChange.WORK_ITEM_TITLE_CHANGED;
            case CARD_MOVED -> BoardChange.WORK_ITEM_MOVED;
            case CARD_REMOVED -> BoardChange.WORK_ITEM_REMOVED;
        };
        return new RecordBoardActivityRequest(activity.position(), change, activity.boardId(), activity.occurredAt(),
                activity.boardName(), activity.columnId(), activity.columnName(), activity.columnPosition(),
                activity.cardId(), activity.cardTitle(), activity.fromColumnId(), activity.toColumnId());
    }
}
