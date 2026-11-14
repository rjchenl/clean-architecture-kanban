package kanban.boardmanagement.usecases.commands.movecolumn;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：調整欄位順序（CA-4）。由 Presenter 實作。 */
public interface MoveColumnOutput {

    void present(CommandResponse response);
}
