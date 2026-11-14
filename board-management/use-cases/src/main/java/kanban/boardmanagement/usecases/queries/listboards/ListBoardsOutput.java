package kanban.boardmanagement.usecases.queries.listboards;

/** Output Boundary：看板清單（CA-4）。由 Presenter 實作。 */
public interface ListBoardsOutput {

    void present(ListBoardsResponse response);
}
