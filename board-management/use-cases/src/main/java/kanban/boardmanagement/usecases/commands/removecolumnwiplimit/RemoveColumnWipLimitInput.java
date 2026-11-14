package kanban.boardmanagement.usecases.commands.removecolumnwiplimit;

/** Input Boundary：取消在製品上限（CA-4）。 */
public interface RemoveColumnWipLimitInput {

    void execute(RemoveColumnWipLimitRequest request);
}
