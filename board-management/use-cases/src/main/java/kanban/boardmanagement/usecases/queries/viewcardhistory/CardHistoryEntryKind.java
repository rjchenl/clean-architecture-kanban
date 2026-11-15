package kanban.boardmanagement.usecases.queries.viewcardhistory;

/** 卡片移動歷程的一筆紀錄是哪一種：新增到某欄、換欄、刪除。 */
public enum CardHistoryEntryKind {
    ADDED,
    MOVED,
    REMOVED
}
