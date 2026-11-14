package kanban.boardmanagement.usecases.commands.changecardcontent.impl;

import kanban.boardmanagement.entities.board.CardDescription;
import kanban.boardmanagement.entities.board.CardId;
import kanban.boardmanagement.entities.board.CardTitle;
import kanban.boardmanagement.usecases.commands.changecardcontent.ChangeCardContentInput;
import kanban.boardmanagement.usecases.commands.changecardcontent.ChangeCardContentOutput;
import kanban.boardmanagement.usecases.commands.changecardcontent.ChangeCardContentRequest;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.support.BoardCommand;

/** 指令 Use Case：修改卡片內容。只做協調，業務規則在看板聚合裡（DDD-9）。 */
public final class ChangeCardContentUseCase implements ChangeCardContentInput {

    private final BoardRepository boards;
    private final TimeSource time;
    private final ChangeCardContentOutput output;

    public ChangeCardContentUseCase(BoardRepository boards, TimeSource time, ChangeCardContentOutput output) {
        this.boards = boards;
        this.time = time;
        this.output = output;
    }

    @Override
    public void execute(ChangeCardContentRequest request) {
        output.present(BoardCommand.onExistingBoard(boards,
                () -> BoardCommand.required(request.boardId(), request.cardId(), request.title()),
                board -> {
                    board.changeCardContent(new CardId(request.cardId()), new CardTitle(request.title()),
                    new CardDescription(request.description()), time.now());
                    return null;
                }));
    }
}
