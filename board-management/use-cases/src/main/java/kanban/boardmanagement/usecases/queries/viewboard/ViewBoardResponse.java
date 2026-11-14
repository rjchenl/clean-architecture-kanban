package kanban.boardmanagement.usecases.queries.viewboard;

/** Response Model：看板現況。找不到時 found 為 false、board 為 null。 */
public record ViewBoardResponse(boolean found, BoardSnapshot board) {
}
