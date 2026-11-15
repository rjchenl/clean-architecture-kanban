package kanban.boardmanagement.usecases.queries.viewboard.impl;

import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshotReader;
import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardInput;
import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardOutput;
import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardRequest;
import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardResponse;

/** 查詢 Use Case：看板現況。只讀讀取模型，不載入聚合、不碰事件紀錄（CQRS-3）。 */
public final class ViewBoardUseCase implements ViewBoardInput {

    private final BoardSnapshotReader boards;
    private final ViewBoardOutput output;

    public ViewBoardUseCase(BoardSnapshotReader boards, ViewBoardOutput output) {
        this.boards = boards;
        this.output = output;
    }

    @Override
    public void execute(ViewBoardRequest request) {
        if (request == null || request.boardId() == null) {
            output.present(new ViewBoardResponse(false, null));
            return;
        }
        output.present(boards.boardById(request.boardId())
                .map(board -> new ViewBoardResponse(true, board))
                .orElseGet(() -> new ViewBoardResponse(false, null)));
    }
}
