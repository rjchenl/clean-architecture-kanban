package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.SetColumnWipLimitInput;
import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.SetColumnWipLimitRequest;

/** Controller：設定在製品上限。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class SetColumnWipLimitController {

    private final SetColumnWipLimitInput input;

    public SetColumnWipLimitController(SetColumnWipLimitInput input) {
        this.input = input;
    }

    public void handle(String boardId, String columnId, Integer limit) {
        if (limit == null) {
            throw new IllegalArgumentException("limit");
        }
        input.execute(new SetColumnWipLimitRequest(boardId, columnId, limit));
    }
}
