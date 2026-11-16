package kanban.fixtures.directrepository.usecases.commands.dothing.impl;

import kanban.fixtures.directrepository.usecases.commands.gateway.BoardRepository;

/** 違規樣本（DDD-5）：只有一個 Repository 欄位，卻用它自己載入、存回兩個看板。 */
public final class DoThingUseCase {

    private final BoardRepository boards;

    DoThingUseCase(BoardRepository boards) {
        this.boards = boards;
    }

    void execute() {
        boards.save(boards.load("board-a"));
        boards.save(boards.load("board-b"));
    }
}
