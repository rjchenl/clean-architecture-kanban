/** 流程分析的第 2 圈 Use Cases：只 requires 流程分析的 Entities（CA-3）；impl、support 套件不公開（CA-16）。 */
module kanban.flowanalysis.usecases {
    requires transitive kanban.flowanalysis.entities;

    exports kanban.flowanalysis.usecases.commands;
    exports kanban.flowanalysis.usecases.commands.gateway;
    exports kanban.flowanalysis.usecases.commands.recordboardactivity;
    exports kanban.flowanalysis.usecases.commands.setcolumnstage;
    exports kanban.flowanalysis.usecases.commands.setstuckthreshold;
    exports kanban.flowanalysis.usecases.commands.removestuckthreshold;
    exports kanban.flowanalysis.usecases.commands.checkstuckworkitems;
    exports kanban.flowanalysis.usecases.eventfeed;
    exports kanban.flowanalysis.usecases.queries.viewflowmetrics;
    exports kanban.flowanalysis.usecases.queries.listboardflows;
}
