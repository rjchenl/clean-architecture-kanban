package kanban.flowanalysis.usecases.queries.listboardflows;

/** 看板流程清單：列出全部看板流程的識別碼，給定時檢查逐一送出。不先挑「有設門檻的」（CQRS-4）。 */
public interface ListBoardFlowsInput {

    void execute();
}
