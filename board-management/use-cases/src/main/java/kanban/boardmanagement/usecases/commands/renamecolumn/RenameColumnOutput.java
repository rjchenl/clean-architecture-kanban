package kanban.boardmanagement.usecases.commands.renamecolumn;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：欄位改名（CA-4）。由 Presenter 實作。 */
public interface RenameColumnOutput {

    void present(CommandResponse response);
}
