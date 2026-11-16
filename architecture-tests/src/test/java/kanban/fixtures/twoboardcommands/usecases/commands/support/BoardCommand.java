package kanban.fixtures.twoboardcommands.usecases.commands.support;

import kanban.fixtures.twoboardcommands.usecases.commands.gateway.BoardRepository;

/** 樣本用的共用流程：載入一個看板、執行、存回去。 */
public final class BoardCommand {

    private BoardCommand() {
    }

    public static void onExistingBoard(BoardRepository boards, String boardId) {
    }
}
