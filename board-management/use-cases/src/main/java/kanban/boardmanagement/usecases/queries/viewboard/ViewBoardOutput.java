package kanban.boardmanagement.usecases.queries.viewboard;

/** Output Boundary：看板現況（CA-4）。由 Presenter 實作。 */
public interface ViewBoardOutput {

    void present(ViewBoardResponse response);
}
