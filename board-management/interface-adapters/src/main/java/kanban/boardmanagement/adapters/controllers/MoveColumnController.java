package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.movecolumn.MoveColumnInput;
import kanban.boardmanagement.usecases.commands.movecolumn.MoveColumnRequest;

/** Controller：調整欄位順序。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class MoveColumnController {

    private final MoveColumnInput input;

    public MoveColumnController(MoveColumnInput input) {
        this.input = input;
    }

    public void handle(String boardId, String columnId, Integer toPosition) {
        if (toPosition == null) {
            throw new IllegalArgumentException("toPosition");
        }
        input.execute(new MoveColumnRequest(boardId, columnId, toPosition));
    }
}
