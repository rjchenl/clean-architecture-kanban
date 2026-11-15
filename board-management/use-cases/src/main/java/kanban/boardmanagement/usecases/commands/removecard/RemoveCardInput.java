package kanban.boardmanagement.usecases.commands.removecard;

/** Input Boundary：刪除卡片（CA-4）。 */
public interface RemoveCardInput {

    void execute(RemoveCardRequest request);
}
