package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.renamecolumn.RenameColumnInput;
import kanban.boardmanagement.usecases.commands.renamecolumn.RenameColumnRequest;

/** Controller：欄位改名。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class RenameColumnController {

    private final RenameColumnInput input;

    public RenameColumnController(RenameColumnInput input) {
        this.input = input;
    }

    public void handle(String boardId, String columnId, String name) {
        input.execute(new RenameColumnRequest(boardId, columnId, name));
    }
}
