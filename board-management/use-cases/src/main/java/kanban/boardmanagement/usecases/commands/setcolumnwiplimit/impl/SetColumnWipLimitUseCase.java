package kanban.boardmanagement.usecases.commands.setcolumnwiplimit.impl;

import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.WipLimit;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.SetColumnWipLimitInput;
import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.SetColumnWipLimitOutput;
import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.SetColumnWipLimitRequest;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：設定在製品上限。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class SetColumnWipLimitUseCase implements SetColumnWipLimitInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final SetColumnWipLimitOutput output;

    public SetColumnWipLimitUseCase(BoardRepository boards, TimeSource time, SetColumnWipLimitOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(SetColumnWipLimitRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.columnId()),
                board -> {
                    board.setColumnWipLimit(new ColumnId(request.columnId()), new WipLimit(request.limit()), time.now());
                    return null;
                }));
    }
}
