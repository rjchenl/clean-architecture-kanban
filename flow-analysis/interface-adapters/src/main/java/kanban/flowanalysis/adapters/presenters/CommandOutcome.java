package kanban.flowanalysis.adapters.presenters;

/** 指令的結果分類，畫面與 HTTP 狀態碼都依它決定。 */
public enum CommandOutcome {
    SUCCEEDED,
    REJECTED,
    NOT_FOUND,
    CONFLICT
}
