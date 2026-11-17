package kanban.boardmanagement.usecases.queries.readboardactivities;

/**
 * 看板動態是哪一種事。只用看板管理自己的用語，不知道下游是誰、下游怎麼用（設計第 4.3、4.4 節）。
 * 只能新增種類，不改舊種類；下游的防腐層對種類逐一列出，新增時會編譯失敗，逼下游明確決定翻譯或略過。
 */
public enum BoardActivityKind {
    BOARD_CREATED,
    BOARD_RENAMED,
    BOARD_ARCHIVED,
    COLUMN_ADDED,
    COLUMN_RENAMED,
    COLUMN_MOVED,
    COLUMN_REMOVED,
    CARD_ADDED,
    CARD_CONTENT_CHANGED,
    CARD_MOVED,
    CARD_REMOVED
}
