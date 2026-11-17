package kanban.flowanalysis.usecases.commands.setstuckthreshold;

/** 設定卡住門檻（F8）。 */
public interface SetStuckThresholdInput {

    void execute(SetStuckThresholdRequest request);
}
