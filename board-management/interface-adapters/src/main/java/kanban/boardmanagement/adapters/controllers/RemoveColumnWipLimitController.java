package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.removecolumnwiplimit.RemoveColumnWipLimitInput;
import kanban.boardmanagement.usecases.commands.removecolumnwiplimit.RemoveColumnWipLimitRequest;

/** Controller：取消在製品上限。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class RemoveColumnWipLimitController {

    private final RemoveColumnWipLimitInput input;

    public RemoveColumnWipLimitController(RemoveColumnWipLimitInput input) {
        this.input = input;
    }

    public void handle(String boardId, String columnId) {
        input.execute(new RemoveColumnWipLimitRequest(boardId, columnId));
    }
}
