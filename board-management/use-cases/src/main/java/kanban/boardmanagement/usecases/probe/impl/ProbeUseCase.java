package kanban.boardmanagement.usecases.probe.impl;

import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.usecases.probe.ProbeInput;

/** 骨架驗證用的探針，Task 7 移除。所在套件不公開，第 3 圈引用不到。 */
public final class ProbeUseCase implements ProbeInput {

    @Override
    public BoardName execute(String value) {
        return new BoardName(value);
    }
}
