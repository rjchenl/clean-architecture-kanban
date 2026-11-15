package kanban.boardmanagement.usecases.commands.archiveboard;

/** Request Model：封存看板。只含簡單資料（CA-5）。 */
public record ArchiveBoardRequest(String boardId) {
}
