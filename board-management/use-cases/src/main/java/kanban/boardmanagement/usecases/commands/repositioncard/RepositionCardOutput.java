package kanban.boardmanagement.usecases.commands.repositioncard;

import kanban.boardmanagement.usecases.commands.CommandResponse;

/** Output Boundary：欄內排序（CA-4）。由 Presenter 實作。 */
public interface RepositionCardOutput {

    void present(CommandResponse response);
}
