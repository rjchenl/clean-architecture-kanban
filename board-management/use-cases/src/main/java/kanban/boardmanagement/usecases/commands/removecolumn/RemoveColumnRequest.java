package kanban.boardmanagement.usecases.commands.removecolumn;

/** Request Model：刪除欄位。只含簡單資料（CA-5）。 */
public record RemoveColumnRequest(String boardId, String columnId) {
}
