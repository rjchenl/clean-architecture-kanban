package kanban.boardmanagement.usecases.commands.repositioncard;

/** Request Model：欄內排序。只含簡單資料（CA-5）。 */
public record RepositionCardRequest(String boardId, String cardId, Integer toPosition) {
}
