package kanban.boardmanagement.usecases.commands.changecardcontent;

/** Input Boundary：修改卡片內容（CA-4）。 */
public interface ChangeCardContentInput {

    void execute(ChangeCardContentRequest request);
}
