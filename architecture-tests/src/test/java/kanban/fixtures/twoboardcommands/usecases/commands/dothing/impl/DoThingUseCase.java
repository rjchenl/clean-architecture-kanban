package kanban.fixtures.twoboardcommands.usecases.commands.dothing.impl;

import kanban.fixtures.twoboardcommands.usecases.commands.gateway.BoardRepository;
import kanban.fixtures.twoboardcommands.usecases.commands.support.BoardCommand;

/** 違規樣本（DDD-5）：只有一個 Repository 欄位，卻對兩個看板各跑一次共用流程。 */
public final class DoThingUseCase {

    private final BoardRepository boards;

    DoThingUseCase(BoardRepository boards) {
        this.boards = boards;
    }

    void execute() {
        BoardCommand.onExistingBoard(boards, "board-a");
        BoardCommand.onExistingBoard(boards, "board-b");
    }
}
