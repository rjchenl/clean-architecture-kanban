package kanban.boardmanagement.usecases.commands.removecolumn.impl;

import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.removecolumn.RemoveColumnInput;
import kanban.boardmanagement.usecases.commands.removecolumn.RemoveColumnOutput;
import kanban.boardmanagement.usecases.commands.removecolumn.RemoveColumnRequest;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：刪除欄位。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class RemoveColumnUseCase implements RemoveColumnInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final RemoveColumnOutput output;

    public RemoveColumnUseCase(BoardRepository boards, TimeSource time, RemoveColumnOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(RemoveColumnRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.columnId()),
                board -> {
                    board.removeColumn(new ColumnId(request.columnId()), time.now());
                    return null;
                }));
    }
}
