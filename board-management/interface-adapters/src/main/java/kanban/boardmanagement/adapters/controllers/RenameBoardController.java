package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.renameboard.RenameBoardInput;
import kanban.boardmanagement.usecases.commands.renameboard.RenameBoardRequest;

/** Controller：看板改名。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class RenameBoardController {

    private final RenameBoardInput input;

    public RenameBoardController(RenameBoardInput input) {
        this.input = input;
    }

    public void handle(String boardId, String name) {
        input.execute(new RenameBoardRequest(boardId, name));
    }
}
