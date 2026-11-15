package kanban.boardmanagement.adapters.presenters;

/** ViewModel：卡片移動歷程的一行。時間與描述都是整理好的文字。 */
public record CardHistoryLineViewModel(String occurredAt, String description) {
}
