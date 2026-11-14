package kanban.boardmanagement.usecases.commands.movecard;

/** Request Model：移動卡片。只含簡單資料（CA-5）。 */
public record MoveCardRequest(String boardId, String cardId, String toColumnId, Integer toPosition) {
}
