package kanban.boardmanagement.entities.board;

import java.time.Instant;

/** 看板事件：看板上已經發生的一件事。過去式命名、不可變（DDD-7）。 */
public sealed interface BoardEvent permits
        BoardCreated, BoardRenamed, BoardArchived,
        ColumnAdded, ColumnRenamed, ColumnMoved, ColumnWipLimitSet, ColumnWipLimitRemoved, ColumnRemoved,
        CardAdded, CardContentChanged, CardMoved, CardRepositioned, CardRemoved {

    BoardId boardId();

    /** 發生時間，由外部傳入聚合，聚合自己不讀時鐘（ES-4）。 */
    Instant occurredAt();
}
