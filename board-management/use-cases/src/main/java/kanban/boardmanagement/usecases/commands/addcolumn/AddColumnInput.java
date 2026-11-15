package kanban.boardmanagement.usecases.commands.addcolumn;

/** Input Boundary：新增欄位（CA-4）。 */
public interface AddColumnInput {

    void execute(AddColumnRequest request);
}
