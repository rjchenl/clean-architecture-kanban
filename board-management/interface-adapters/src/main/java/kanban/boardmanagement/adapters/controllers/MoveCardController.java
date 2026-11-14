package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.movecard.MoveCardInput;
import kanban.boardmanagement.usecases.commands.movecard.MoveCardRequest;

/** Controller：移動卡片。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class MoveCardController {

    private final MoveCardInput input;

    public MoveCardController(MoveCardInput input) {
        this.input = input;
    }

    public void handle(String boardId, String cardId, String toColumnId, Integer toPosition) {
        if (toPosition == null) {
            throw new IllegalArgumentException("toPosition");
        }
        input.execute(new MoveCardRequest(boardId, cardId, toColumnId, toPosition));
    }
}
