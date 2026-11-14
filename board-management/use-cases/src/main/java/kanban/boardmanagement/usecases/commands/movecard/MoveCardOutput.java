package kanban.boardmanagement.usecases.commands.movecard;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：移動卡片（CA-4）。由 Presenter 實作。 */
public interface MoveCardOutput {

    void present(CommandResponse response);
}
