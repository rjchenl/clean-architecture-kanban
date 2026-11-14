package kanban.boardmanagement.usecases.commands.removecolumnwiplimit;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：取消在製品上限（CA-4）。由 Presenter 實作。 */
public interface RemoveColumnWipLimitOutput {

    void present(CommandResponse response);
}
