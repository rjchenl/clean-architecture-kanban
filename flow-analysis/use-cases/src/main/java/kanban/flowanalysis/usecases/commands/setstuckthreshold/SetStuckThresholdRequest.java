package kanban.flowanalysis.usecases.commands.setstuckthreshold;

/** @param days 1～365 天 */
public record SetStuckThresholdRequest(String boardFlowId, Integer days) {
}
