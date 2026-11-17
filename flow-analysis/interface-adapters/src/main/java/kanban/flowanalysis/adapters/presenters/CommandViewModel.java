package kanban.flowanalysis.adapters.presenters;

/** 指令的 ViewModel：結果、給使用者看的訊息（成功時是空字串）、新建立的識別碼（沒有時是空字串）。 */
public record CommandViewModel(CommandOutcome outcome, String message, String createdId) {
}
