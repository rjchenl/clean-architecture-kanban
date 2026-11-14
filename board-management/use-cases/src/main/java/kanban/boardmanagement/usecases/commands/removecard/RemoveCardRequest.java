package kanban.boardmanagement.usecases.commands.removecard;

/** Request Model：刪除卡片。只含簡單資料（CA-5）。 */
public record RemoveCardRequest(String boardId, String cardId) {
}
