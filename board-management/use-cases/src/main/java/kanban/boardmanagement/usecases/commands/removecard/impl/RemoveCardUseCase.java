package kanban.boardmanagement.usecases.commands.removecard.impl;

import kanban.boardmanagement.entities.board.CardId;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.removecard.RemoveCardInput;
import kanban.boardmanagement.usecases.commands.removecard.RemoveCardOutput;
import kanban.boardmanagement.usecases.commands.removecard.RemoveCardRequest;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：刪除卡片。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class RemoveCardUseCase implements RemoveCardInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final RemoveCardOutput output;

    public RemoveCardUseCase(BoardRepository boards, TimeSource time, RemoveCardOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(RemoveCardRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.cardId()),
                board -> {
                    board.removeCard(new CardId(request.cardId()), time.now());
                    return null;
                }));
    }
}
