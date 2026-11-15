package kanban.boardmanagement.usecases.commands.addcolumn;

/** Request Model：新增欄位。只含簡單資料（CA-5）。 */
public record AddColumnRequest(String boardId, String name) {
}
