package kanban.boardmanagement.usecases.queries.viewboard;

/** Input Boundary：看板現況（CA-4）。 */
public interface ViewBoardInput {

    void execute(ViewBoardRequest request);
}
