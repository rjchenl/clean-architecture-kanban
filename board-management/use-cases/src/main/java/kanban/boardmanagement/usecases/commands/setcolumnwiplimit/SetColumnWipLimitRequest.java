package kanban.boardmanagement.usecases.commands.setcolumnwiplimit;

/** Request Model：設定在製品上限。只含簡單資料（CA-5）。 */
public record SetColumnWipLimitRequest(String boardId, String columnId, Integer limit) {
}
