package kanban.boardmanagement.usecases.commands.createboard;

/** Request Model：建立看板。只含簡單資料（CA-5）。 */
public record CreateBoardRequest(String name) {
}
