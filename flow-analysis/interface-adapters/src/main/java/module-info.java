/** 流程分析的第 3 圈 Interface Adapters：只能用 JDK（含 java.sql）、流程分析的內兩圈與 Jackson（CA-7）；不認識看板管理（DDD-3）。 */
module kanban.flowanalysis.adapters {
    requires transitive kanban.flowanalysis.usecases;
    requires java.sql;
    requires tools.jackson.databind;

    exports kanban.flowanalysis.adapters.eventstore;
    exports kanban.flowanalysis.adapters.system;
}
