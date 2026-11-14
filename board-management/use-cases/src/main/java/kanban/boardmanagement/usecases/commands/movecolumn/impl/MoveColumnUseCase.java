package kanban.boardmanagement.usecases.commands.movecolumn.impl;

import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.Position;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.movecolumn.MoveColumnInput;
import kanban.boardmanagement.usecases.commands.movecolumn.MoveColumnOutput;
import kanban.boardmanagement.usecases.commands.movecolumn.MoveColumnRequest;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：調整欄位順序。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class MoveColumnUseCase implements MoveColumnInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final MoveColumnOutput output;

    public MoveColumnUseCase(BoardRepository boards, TimeSource time, MoveColumnOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(MoveColumnRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.columnId()),
                board -> {
                    board.moveColumn(new ColumnId(request.columnId()), new Position(request.toPosition()), time.now());
                    return null;
                }));
    }
}
