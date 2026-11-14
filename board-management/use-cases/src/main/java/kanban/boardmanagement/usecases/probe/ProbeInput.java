package kanban.boardmanagement.usecases.probe;

import kanban.boardmanagement.entities.board.BoardName;

/** 骨架驗證用的探針，Task 7 移除。 */
public interface ProbeInput {

    BoardName execute(String value);
}
