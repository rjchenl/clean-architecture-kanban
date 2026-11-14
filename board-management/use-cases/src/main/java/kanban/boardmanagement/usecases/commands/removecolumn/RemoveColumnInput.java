package kanban.boardmanagement.usecases.commands.removecolumn;

/** Input Boundary：刪除欄位（CA-4）。 */
public interface RemoveColumnInput {

    void execute(RemoveColumnRequest request);
}
