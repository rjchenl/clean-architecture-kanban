package kanban.boardmanagement.usecases.commands.archiveboard;

/** Input Boundary：封存看板（CA-4）。 */
public interface ArchiveBoardInput {

    void execute(ArchiveBoardRequest request);
}
