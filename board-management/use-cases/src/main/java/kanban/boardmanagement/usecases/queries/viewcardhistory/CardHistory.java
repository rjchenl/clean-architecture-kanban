package kanban.boardmanagement.usecases.queries.viewcardhistory;

import java.util.List;

/** 讀取模型：一張卡片的移動歷程，為「卡片移動歷程」畫面準備（CQRS-3）。 */
public record CardHistory(String cardId, String title, List<CardHistoryEntry> entries) {
}
