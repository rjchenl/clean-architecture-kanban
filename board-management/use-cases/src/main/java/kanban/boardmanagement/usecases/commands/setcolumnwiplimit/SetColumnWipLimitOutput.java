package kanban.boardmanagement.usecases.commands.setcolumnwiplimit;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：設定在製品上限（CA-4）。由 Presenter 實作。 */
public interface SetColumnWipLimitOutput {

    void present(CommandResponse response);
}
