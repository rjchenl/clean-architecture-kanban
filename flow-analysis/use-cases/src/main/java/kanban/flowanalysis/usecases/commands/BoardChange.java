package kanban.flowanalysis.usecases.commands;

/**
 * 看板上發生的是哪一種事，用流程分析的用語（防腐層把看板管理的「看板動態種類」翻成這個）。
 * 每一種變化需要的欄位（其餘為 null）：
 * <ul>
 *   <li>BOARD_FLOW_CREATED、BOARD_FLOW_RENAMED：boardName</li>
 *   <li>BOARD_FLOW_ARCHIVED：無</li>
 *   <li>FLOW_COLUMN_ADDED、FLOW_COLUMN_RENAMED：flowColumnId、flowColumnName</li>
 *   <li>FLOW_COLUMN_MOVED：flowColumnId、flowColumnPosition</li>
 *   <li>FLOW_COLUMN_REMOVED：flowColumnId</li>
 *   <li>WORK_ITEM_ADDED：workItemId、flowColumnId、workItemTitle</li>
 *   <li>WORK_ITEM_TITLE_CHANGED：workItemId、workItemTitle</li>
 *   <li>WORK_ITEM_MOVED：workItemId、fromFlowColumnId、toFlowColumnId</li>
 *   <li>WORK_ITEM_REMOVED：workItemId、flowColumnId（被刪除時所在的欄）</li>
 * </ul>
 */
public enum BoardChange {
    BOARD_FLOW_CREATED,
    BOARD_FLOW_RENAMED,
    BOARD_FLOW_ARCHIVED,
    FLOW_COLUMN_ADDED,
    FLOW_COLUMN_RENAMED,
    FLOW_COLUMN_MOVED,
    FLOW_COLUMN_REMOVED,
    WORK_ITEM_ADDED,
    WORK_ITEM_TITLE_CHANGED,
    WORK_ITEM_MOVED,
    WORK_ITEM_REMOVED
}
