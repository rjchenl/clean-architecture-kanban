package kanban.flowanalysis.usecases.commands.recordboardactivity;

/** 記錄看板動態：把看板上發生的一件事交給看板流程；看板流程還不存在就先建立（防腐層使用）。 */
public interface RecordBoardActivityInput {

    void execute(RecordBoardActivityRequest request);
}
