package kanban.boardmanagement.usecases.commands.createboard;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：建立看板（CA-4）。由 Presenter 實作。 */
public interface CreateBoardOutput {

    void present(CommandResponse response);
}
