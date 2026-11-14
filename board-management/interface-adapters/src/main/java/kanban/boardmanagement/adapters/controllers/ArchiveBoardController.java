package kanban.boardmanagement.adapters.controllers;

import kanban.boardmanagement.usecases.commands.archiveboard.ArchiveBoardInput;
import kanban.boardmanagement.usecases.commands.archiveboard.ArchiveBoardRequest;

/** Controller：封存看板。把原始輸入組成 Request Model，交給 Input Boundary；不認識 Presenter（CA-8）。 */
public final class ArchiveBoardController {

    private final ArchiveBoardInput input;

    public ArchiveBoardController(ArchiveBoardInput input) {
        this.input = input;
    }

    public void handle(String boardId) {
        input.execute(new ArchiveBoardRequest(boardId));
    }
}
