package kanban.boardmanagement.usecases.commands.renameboard;

/** Request Model：看板改名。只含簡單資料（CA-5）。 */
public record RenameBoardRequest(String boardId, String name) {
}
