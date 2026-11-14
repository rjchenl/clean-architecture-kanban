package kanban.boardmanagement.usecases.commands.movecolumn;

/** Input Boundary：調整欄位順序（CA-4）。 */
public interface MoveColumnInput {

    void execute(MoveColumnRequest request);
}
