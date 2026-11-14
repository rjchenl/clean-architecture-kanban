package kanban.boardmanagement.usecases.commands.removecolumnwiplimit.impl;

import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.removecolumnwiplimit.RemoveColumnWipLimitInput;
import kanban.boardmanagement.usecases.commands.removecolumnwiplimit.RemoveColumnWipLimitOutput;
import kanban.boardmanagement.usecases.commands.removecolumnwiplimit.RemoveColumnWipLimitRequest;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：取消在製品上限。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class RemoveColumnWipLimitUseCase implements RemoveColumnWipLimitInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final RemoveColumnWipLimitOutput output;

    public RemoveColumnWipLimitUseCase(BoardRepository boards, TimeSource time, RemoveColumnWipLimitOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(RemoveColumnWipLimitRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.columnId()),
                board -> {
                    board.removeColumnWipLimit(new ColumnId(request.columnId()), time.now());
                    return null;
                }));
    }
}
