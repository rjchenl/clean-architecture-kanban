package kanban.boardmanagement.usecases.commands.renameboard;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：看板改名（CA-4）。由 Presenter 實作。 */
public interface RenameBoardOutput {

    void present(CommandResponse response);
}
