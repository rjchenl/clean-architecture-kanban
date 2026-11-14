package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.createboard.CreateBoardInput;
import kanban.boardmanagement.usecases.commands.createboard.CreateBoardRequest;

/** Controller：建立看板。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class CreateBoardController {

    private final CreateBoardInput input;

    public CreateBoardController(CreateBoardInput input) {
        this.input = input;
    }

    public void handle(String name) {
        input.execute(new CreateBoardRequest(name));
    }
}
