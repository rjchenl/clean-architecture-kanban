package kanban.flowanalysis.usecases.commands.setcolumnstage;

/** @param stage 階段的名稱：UNCLASSIFIED、NOT_STARTED、IN_PROGRESS、DONE */
public record SetColumnStageRequest(String boardFlowId, String flowColumnId, String stage) {
}
