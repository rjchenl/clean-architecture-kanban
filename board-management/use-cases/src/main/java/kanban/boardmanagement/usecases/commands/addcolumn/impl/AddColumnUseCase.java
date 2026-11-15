package kanban.boardmanagement.usecases.commands.addcolumn.impl;

import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.usecases.commands.addcolumn.AddColumnInput;
import kanban.boardmanagement.usecases.commands.addcolumn.AddColumnOutput;
import kanban.boardmanagement.usecases.commands.addcolumn.AddColumnRequest;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.IdentityGenerator;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：新增欄位。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class AddColumnUseCase implements AddColumnInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final IdentityGenerator ids;
    private final AddColumnOutput output;

    public AddColumnUseCase(BoardRepository boards, TimeSource time, IdentityGenerator ids, AddColumnOutput output) {
        this.boards = boards;
        this.time = time;
        this.ids = ids;
        this.output = output;
    }

    @Override
    public void execute(AddColumnRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.name()),
                board -> {
            String newId = ids.nextId();
                    board.addColumn(new ColumnId(newId), new ColumnName(request.name()), time.now());
                    return newId;
                }));
    }
}
