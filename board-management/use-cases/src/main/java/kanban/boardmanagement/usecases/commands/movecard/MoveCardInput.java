package kanban.boardmanagement.usecases.commands.movecard;

/** Input Boundary：移動卡片（CA-4）。 */
public interface MoveCardInput {

    void execute(MoveCardRequest request);
}
