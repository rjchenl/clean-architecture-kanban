package kanban.boardmanagement.usecases.commands.addcard;

/** Request Model：新增卡片。只含簡單資料（CA-5）。 */
public record AddCardRequest(String boardId, String columnId, String title, String description) {
}
