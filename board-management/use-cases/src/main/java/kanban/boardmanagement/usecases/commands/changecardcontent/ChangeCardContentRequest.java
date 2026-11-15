package kanban.boardmanagement.usecases.commands.changecardcontent;

/** Request Model：修改卡片內容。只含簡單資料（CA-5）。 */
public record ChangeCardContentRequest(String boardId, String cardId, String title, String description) {
}
