package kanban.boardmanagement.usecases.commands.renameboard;

/** Input Boundary：看板改名（CA-4）。 */
public interface RenameBoardInput {

    void execute(RenameBoardRequest request);
}
