package kanban.boardmanagement.usecases.commands.repositioncard;

/** Input Boundary：欄內排序（CA-4）。 */
public interface RepositionCardInput {

    void execute(RepositionCardRequest request);
}
