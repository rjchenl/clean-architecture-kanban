package kanban.boardmanagement.usecases.commands.removecolumnwiplimit;

/** Request Model：取消在製品上限。只含簡單資料（CA-5）。 */
public record RemoveColumnWipLimitRequest(String boardId, String columnId) {
}
