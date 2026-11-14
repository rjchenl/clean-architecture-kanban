package kanban.boardmanagement.usecases.queries.viewcardhistory;

/** Response Model：卡片移動歷程。找不到時 found 為 false、history 為 null。 */
public record ViewCardHistoryResponse(boolean found, CardHistory history) {
}
