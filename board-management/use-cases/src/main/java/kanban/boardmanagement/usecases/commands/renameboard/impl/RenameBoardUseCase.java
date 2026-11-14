package kanban.boardmanagement.usecases.commands.renameboard.impl;

import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.renameboard.RenameBoardInput;
import kanban.boardmanagement.usecases.commands.renameboard.RenameBoardOutput;
import kanban.boardmanagement.usecases.commands.renameboard.RenameBoardRequest;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：看板改名。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class RenameBoardUseCase implements RenameBoardInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final RenameBoardOutput output;

    public RenameBoardUseCase(BoardRepository boards, TimeSource time, RenameBoardOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(RenameBoardRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.name()),
                board -> {
                    board.rename(new BoardName(request.name()), time.now());
                    return null;
                }));
    }
}
