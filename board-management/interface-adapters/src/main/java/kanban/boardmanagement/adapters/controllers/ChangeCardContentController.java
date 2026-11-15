package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.changecardcontent.ChangeCardContentInput;
import kanban.boardmanagement.usecases.commands.changecardcontent.ChangeCardContentRequest;

/** Controller：修改卡片內容。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class ChangeCardContentController {

    private final ChangeCardContentInput input;

    public ChangeCardContentController(ChangeCardContentInput input) {
        this.input = input;
    }

    public void handle(String boardId, String cardId, String title, String description) {
        input.execute(new ChangeCardContentRequest(boardId, cardId, title, description));
    }
}
