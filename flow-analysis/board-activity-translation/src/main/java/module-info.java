/**
 * 流程分析的防腐層（DDD-3）：唯一認識看板管理的模組。
 *
 * <p>看板管理的 Use Cases 模組 requires transitive 它的 Entities，所以這個模組在編譯時看得到看板管理的領域類別；
 * 只能引用看板管理公布的看板動態，由架構測試擋（設計第 0 節 4a）。
 */
module kanban.flowanalysis.boardactivity {
    requires kanban.flowanalysis.usecases;
    requires kanban.boardmanagement.usecases;
    requires java.sql;

    exports kanban.flowanalysis.adapters.boardactivity;
}
