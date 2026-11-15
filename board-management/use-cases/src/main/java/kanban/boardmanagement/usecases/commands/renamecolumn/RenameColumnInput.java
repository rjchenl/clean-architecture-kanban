package kanban.boardmanagement.usecases.commands.renamecolumn;

/** Input Boundary：欄位改名（CA-4）。 */
public interface RenameColumnInput {

    void execute(RenameColumnRequest request);
}
