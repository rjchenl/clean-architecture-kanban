package kanban.boardmanagement.usecases.commands.removecard;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：刪除卡片（CA-4）。由 Presenter 實作。 */
public interface RemoveCardOutput {

    void present(CommandResponse response);
}
