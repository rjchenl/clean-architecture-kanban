package kanban.boardmanagement.usecases.commands.archiveboard.impl;

import kanban.boardmanagement.usecases.commands.archiveboard.ArchiveBoardInput;
import kanban.boardmanagement.usecases.commands.archiveboard.ArchiveBoardOutput;
import kanban.boardmanagement.usecases.commands.archiveboard.ArchiveBoardRequest;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：封存看板。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class ArchiveBoardUseCase implements ArchiveBoardInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final ArchiveBoardOutput output;

    public ArchiveBoardUseCase(BoardRepository boards, TimeSource time, ArchiveBoardOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(ArchiveBoardRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId()),
                board -> {
                    board.archive(time.now());
                    return null;
                }));
    }
}
