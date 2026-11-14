package kanban.boardmanagement.usecases.commands.movecard.impl;

import kanban.boardmanagement.entities.board.CardId;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.Position;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.movecard.MoveCardInput;
import kanban.boardmanagement.usecases.commands.movecard.MoveCardOutput;
import kanban.boardmanagement.usecases.commands.movecard.MoveCardRequest;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：移動卡片。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class MoveCardUseCase implements MoveCardInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final MoveCardOutput output;

    public MoveCardUseCase(BoardRepository boards, TimeSource time, MoveCardOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(MoveCardRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.cardId(), request.toColumnId(), request.toPosition()),
                board -> {
                    board.moveCard(new CardId(request.cardId()), new ColumnId(request.toColumnId()),
                    new Position(request.toPosition()), time.now());
                    return null;
                }));
    }
}
