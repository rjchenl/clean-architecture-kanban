package kanban.boardmanagement.usecases.queries.viewcardhistory;

/** Request Model：卡片移動歷程。 */
public record ViewCardHistoryRequest(String boardId, String cardId) {
}
