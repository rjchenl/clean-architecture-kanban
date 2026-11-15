package kanban.boardmanagement.usecases.commands.setcolumnwiplimit;

/** Input Boundary：設定在製品上限（CA-4）。 */
public interface SetColumnWipLimitInput {

    void execute(SetColumnWipLimitRequest request);
}
