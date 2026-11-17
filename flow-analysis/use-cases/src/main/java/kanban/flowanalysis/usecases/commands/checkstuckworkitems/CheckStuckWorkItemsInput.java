package kanban.flowanalysis.usecases.commands.checkstuckworkitems;

/** 檢查卡住的工作項目：定時帶入現在的時間，判斷哪些工作項目卡住（F10）。 */
public interface CheckStuckWorkItemsInput {

    void execute(CheckStuckWorkItemsRequest request);
}
