package kanban.boardmanagement.usecases.queries.viewcardhistory;

/** Input Boundary：卡片移動歷程（CA-4）。 */
public interface ViewCardHistoryInput {

    void execute(ViewCardHistoryRequest request);
}
