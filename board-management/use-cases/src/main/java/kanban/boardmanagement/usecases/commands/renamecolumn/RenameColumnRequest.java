package kanban.boardmanagement.usecases.commands.renamecolumn;

/** Request Model：欄位改名。只含簡單資料（CA-5）。 */
public record RenameColumnRequest(String boardId, String columnId, String name) {
}
