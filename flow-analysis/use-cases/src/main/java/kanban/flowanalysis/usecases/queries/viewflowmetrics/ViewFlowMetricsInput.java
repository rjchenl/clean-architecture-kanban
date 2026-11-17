package kanban.flowanalysis.usecases.queries.viewflowmetrics;

/** 查看流程數字：一個看板的階段設定、各欄數字、最近 8 週、卡住清單。 */
public interface ViewFlowMetricsInput {

    void execute(ViewFlowMetricsRequest request);
}
