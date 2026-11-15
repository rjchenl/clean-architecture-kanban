package kanban.boardmanagement.usecases.commands.renamecolumn.impl;

import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.renamecolumn.RenameColumnInput;
import kanban.boardmanagement.usecases.commands.renamecolumn.RenameColumnOutput;
import kanban.boardmanagement.usecases.commands.renamecolumn.RenameColumnRequest;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：欄位改名。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class RenameColumnUseCase implements RenameColumnInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final RenameColumnOutput output;

    public RenameColumnUseCase(BoardRepository boards, TimeSource time, RenameColumnOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(RenameColumnRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.columnId(), request.name()),
                board -> {
                    board.renameColumn(new ColumnId(request.columnId()), new ColumnName(request.name()), time.now());
                    return null;
                }));
    }
}
