package kanban.boardmanagement.usecases.commands.createboard;

/** Input Boundary：建立看板（CA-4）。 */
public interface CreateBoardInput {

    void execute(CreateBoardRequest request);
}
