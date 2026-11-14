package kanban.boardmanagement.usecases.commands.repositioncard.impl;

import kanban.boardmanagement.entities.board.CardId;
import kanban.boardmanagement.entities.board.Position;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.repositioncard.RepositionCardInput;
import kanban.boardmanagement.usecases.commands.repositioncard.RepositionCardOutput;
import kanban.boardmanagement.usecases.commands.repositioncard.RepositionCardRequest;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：欄內排序。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class RepositionCardUseCase implements RepositionCardInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final RepositionCardOutput output;

    public RepositionCardUseCase(BoardRepository boards, TimeSource time, RepositionCardOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(RepositionCardRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.cardId()),
                board -> {
                    board.repositionCard(new CardId(request.cardId()), new Position(request.toPosition()), time.now());
                    return null;
                }));
    }
}
