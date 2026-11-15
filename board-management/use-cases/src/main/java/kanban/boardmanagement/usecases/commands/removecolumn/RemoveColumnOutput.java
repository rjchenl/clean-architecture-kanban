package kanban.boardmanagement.usecases.commands.removecolumn;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：刪除欄位（CA-4）。由 Presenter 實作。 */
public interface RemoveColumnOutput {

    void present(CommandResponse response);
}
