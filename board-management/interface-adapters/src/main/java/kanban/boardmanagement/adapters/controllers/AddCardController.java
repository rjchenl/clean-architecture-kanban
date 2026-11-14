package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.addcard.AddCardInput;
import kanban.boardmanagement.usecases.commands.addcard.AddCardRequest;

/** Controller：新增卡片。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class AddCardController {

    private final AddCardInput input;

    public AddCardController(AddCardInput input) {
        this.input = input;
    }

    public void handle(String boardId, String columnId, String title, String description) {
        input.execute(new AddCardRequest(boardId, columnId, title, description));
    }
}
