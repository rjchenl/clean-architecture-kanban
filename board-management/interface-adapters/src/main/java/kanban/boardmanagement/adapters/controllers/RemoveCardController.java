package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.removecard.RemoveCardInput;
import kanban.boardmanagement.usecases.commands.removecard.RemoveCardRequest;

/** Controller：刪除卡片。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class RemoveCardController {

    private final RemoveCardInput input;

    public RemoveCardController(RemoveCardInput input) {
        this.input = input;
    }

    public void handle(String boardId, String cardId) {
        input.execute(new RemoveCardRequest(boardId, cardId));
    }
}
