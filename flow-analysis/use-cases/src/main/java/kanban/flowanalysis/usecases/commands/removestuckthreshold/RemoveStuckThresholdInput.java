package kanban.flowanalysis.usecases.commands.removestuckthreshold;

/** 取消卡住門檻（F8）。 */
public interface RemoveStuckThresholdInput {

    void execute(RemoveStuckThresholdRequest request);
}
