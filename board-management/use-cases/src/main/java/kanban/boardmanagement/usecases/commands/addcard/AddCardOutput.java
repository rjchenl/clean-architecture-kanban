package kanban.boardmanagement.usecases.commands.addcard;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：新增卡片（CA-4）。由 Presenter 實作。 */
public interface AddCardOutput {

    void present(CommandResponse response);
}
