package kanban.boardmanagement.usecases.commands.addcolumn;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：新增欄位（CA-4）。由 Presenter 實作。 */
public interface AddColumnOutput {

    void present(CommandResponse response);
}
