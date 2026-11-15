package kanban.boardmanagement.usecases.commands.archiveboard;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：封存看板（CA-4）。由 Presenter 實作。 */
public interface ArchiveBoardOutput {

    void present(CommandResponse response);
}
