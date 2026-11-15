package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.removecolumn.RemoveColumnInput;
import kanban.boardmanagement.usecases.commands.removecolumn.RemoveColumnRequest;

/** Controller：刪除欄位。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class RemoveColumnController {

    private final RemoveColumnInput input;

    public RemoveColumnController(RemoveColumnInput input) {
        this.input = input;
    }

    public void handle(String boardId, String columnId) {
        input.execute(new RemoveColumnRequest(boardId, columnId));
    }
}
