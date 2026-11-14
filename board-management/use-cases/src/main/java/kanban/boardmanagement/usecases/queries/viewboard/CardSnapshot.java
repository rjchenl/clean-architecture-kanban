package kanban.boardmanagement.usecases.queries.viewboard;

/** 讀取模型：看板現況裡的一張卡片。 */
public record CardSnapshot(String cardId, String title, String description) {
}
