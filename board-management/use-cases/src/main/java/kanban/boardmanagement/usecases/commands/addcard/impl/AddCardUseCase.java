package kanban.boardmanagement.usecases.commands.addcard.impl;

import kanban.boardmanagement.entities.board.CardDescription;
import kanban.boardmanagement.entities.board.CardId;
import kanban.boardmanagement.entities.board.CardTitle;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.usecases.commands.addcard.AddCardInput;
import kanban.boardmanagement.usecases.commands.addcard.AddCardOutput;
import kanban.boardmanagement.usecases.commands.addcard.AddCardRequest;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.IdentityGenerator;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：新增卡片。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class AddCardUseCase implements AddCardInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final IdentityGenerator ids;
    private final AddCardOutput output;

    public AddCardUseCase(BoardRepository boards, TimeSource time, IdentityGenerator ids, AddCardOutput output) {
        this.boards = boards;
        this.time = time;
        this.ids = ids;
        this.output = output;
    }

    @Override
    public void execute(AddCardRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.columnId(), request.title()),
                board -> {
            String newId = ids.nextId();
                    board.addCard(new CardId(newId), new ColumnId(request.columnId()), new CardTitle(request.title()),
                    new CardDescription(request.description()), time.now());
                    return newId;
                }));
    }
}
