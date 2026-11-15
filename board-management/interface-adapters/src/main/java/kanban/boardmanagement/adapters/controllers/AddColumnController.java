package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.addcolumn.AddColumnInput;
import kanban.boardmanagement.usecases.commands.addcolumn.AddColumnRequest;

/** Controller：新增欄位。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class AddColumnController {

    private final AddColumnInput input;

    public AddColumnController(AddColumnInput input) {
        this.input = input;
    }

    public void handle(String boardId, String name) {
        input.execute(new AddColumnRequest(boardId, name));
    }
}
