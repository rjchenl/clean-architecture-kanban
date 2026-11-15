package kanban.boardmanagement.usecases.queries.viewcardhistory;

/** Output Boundary：卡片移動歷程（CA-4）。由 Presenter 實作。 */
public interface ViewCardHistoryOutput {

    void present(ViewCardHistoryResponse response);
}
