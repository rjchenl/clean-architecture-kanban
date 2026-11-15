package kanban.boardmanagement.usecases.commands.movecolumn;

/** Request Model：調整欄位順序。只含簡單資料（CA-5）。 */
public record MoveColumnRequest(String boardId, String columnId, Integer toPosition) {
}
