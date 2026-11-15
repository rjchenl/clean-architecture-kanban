package kanban.boardmanagement.usecases.commands.addcard;

/** Input Boundary：新增卡片（CA-4）。 */
public interface AddCardInput {

    void execute(AddCardRequest request);
}
