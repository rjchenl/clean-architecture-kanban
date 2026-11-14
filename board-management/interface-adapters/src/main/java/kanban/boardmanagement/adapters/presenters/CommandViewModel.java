package kanban.boardmanagement.adapters.presenters;

/** ViewModel：一個指令的結果。message 是可以直接顯示的中文訊息；沒有訊息或識別碼時是空字串。 */
public record CommandViewModel(CommandOutcome outcome, String message, String createdId) {
}
