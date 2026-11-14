package kanban.boardmanagement.usecases.commands.createboard.impl;

import kanban.boardmanagement.entities.board.Board;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.usecases.commands.createboard.CreateBoardInput;
import kanban.boardmanagement.usecases.commands.createboard.CreateBoardOutput;
import kanban.boardmanagement.usecases.commands.createboard.CreateBoardRequest;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.IdentityGenerator;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：建立看板。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class CreateBoardUseCase implements CreateBoardInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final IdentityGenerator ids;
    private final CreateBoardOutput output;

    public CreateBoardUseCase(BoardRepository boards, TimeSource time, IdentityGenerator ids, CreateBoardOutput output) {
        this.boards = boards;
        this.time = time;
        this.ids = ids;
        this.output = output;
    }

    @Override
    public void execute(CreateBoardRequest request) {
        output.present(BoardCommand.onNewBoard(boards,
                () -> BoardCommand.required(request.name()),
                () -> Board.create(new BoardId(ids.nextId()), new BoardName(request.name()), time.now())));
    }
}
