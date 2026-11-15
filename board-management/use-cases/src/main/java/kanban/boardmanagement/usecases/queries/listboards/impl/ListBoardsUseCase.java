package kanban.boardmanagement.usecases.queries.listboards.impl;

import kanban.boardmanagement.usecases.queries.listboards.BoardSummaryReader;
import kanban.boardmanagement.usecases.queries.listboards.ListBoardsInput;
import kanban.boardmanagement.usecases.queries.listboards.ListBoardsOutput;
import kanban.boardmanagement.usecases.queries.listboards.ListBoardsResponse;

/** 查詢 Use Case：看板清單。 */
public final class ListBoardsUseCase implements ListBoardsInput {

    private final BoardSummaryReader boards;
    private final ListBoardsOutput output;

    public ListBoardsUseCase(BoardSummaryReader boards, ListBoardsOutput output) {
        this.boards = boards;
        this.output = output;
    }

    @Override
    public void execute() {
        output.present(new ListBoardsResponse(boards.allBoards()));
    }
}
