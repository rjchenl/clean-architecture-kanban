package kanban.boardmanagement.adapters.presenters;

/** 指令的結果種類。最外圈依它一對一換算成 HTTP 狀態碼，不做其他判斷。 */
public enum CommandOutcome {
    SUCCEEDED,
    REJECTED,
    NOT_FOUND,
    CONFLICT
}
