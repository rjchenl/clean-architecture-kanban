package kanban.boardmanagement.usecases.commands.changecardcontent;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：修改卡片內容（CA-4）。由 Presenter 實作。 */
public interface ChangeCardContentOutput {

    void present(CommandResponse response);
}
