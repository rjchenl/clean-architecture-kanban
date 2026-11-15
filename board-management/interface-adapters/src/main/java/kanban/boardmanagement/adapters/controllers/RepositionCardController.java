package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.repositioncard.RepositionCardInput;
import kanban.boardmanagement.usecases.commands.repositioncard.RepositionCardRequest;

/** Controller：欄內排序。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class RepositionCardController {

    private final RepositionCardInput input;

    public RepositionCardController(RepositionCardInput input) {
        this.input = input;
    }

    public void handle(String boardId, String cardId, Integer toPosition) {
        input.execute(new RepositionCardRequest(boardId, cardId, toPosition));
    }
}
