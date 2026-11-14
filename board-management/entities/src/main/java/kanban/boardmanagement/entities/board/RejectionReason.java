package kanban.boardmanagement.entities.board;

/** 拒絕原因：指令被拒絕時，違反的是哪一條規則。 */
public enum RejectionReason {
    IDENTIFIER_BLANK,
    NAME_BLANK,
    NAME_TOO_LONG,
    DESCRIPTION_TOO_LONG,
    WIP_LIMIT_NOT_POSITIVE,
    POSITION_OUT_OF_RANGE,
    BOARD_ARCHIVED,
    COLUMN_NOT_FOUND,
    CARD_NOT_FOUND,
    COLUMN_NAME_DUPLICATED,
    COLUMN_NOT_EMPTY,
    WIP_LIMIT_EXCEEDED,
    WIP_LIMIT_BELOW_CARD_COUNT
}
