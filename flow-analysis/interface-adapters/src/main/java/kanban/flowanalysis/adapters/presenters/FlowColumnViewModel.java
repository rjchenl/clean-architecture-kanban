package kanban.flowanalysis.adapters.presenters;

/** @param stage 給下拉選單比對用的階段名稱；畫面顯示 stageLabel */
public record FlowColumnViewModel(String flowColumnId, String name, String stage, String stageLabel,
        String workItemCount, String averageDwell) {
}
