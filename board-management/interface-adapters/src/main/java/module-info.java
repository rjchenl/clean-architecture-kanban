/** 第 3 圈 Interface Adapters：只能用 JDK（含 java.sql）、內兩圈與 Jackson（CA-7）。 */
module kanban.boardmanagement.adapters {
    requires transitive kanban.boardmanagement.usecases;
    requires java.sql;
    requires tools.jackson.databind;

    exports kanban.boardmanagement.adapters.eventstore;
    exports kanban.boardmanagement.adapters.system;
}
