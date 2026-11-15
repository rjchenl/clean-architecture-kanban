package kanban.fixtures.contexts.flowanalytics.usecases;

import kanban.fixtures.contexts.boardmanagement.entities.BoardThing;

/** 違規樣本（DDD-2）：「流程分析」情境直接引用「看板管理」的領域類別。 */
public class UsesBoardThing {
    BoardThing borrowed;
}
