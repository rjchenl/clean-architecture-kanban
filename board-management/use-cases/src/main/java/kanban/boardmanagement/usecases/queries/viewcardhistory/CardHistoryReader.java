package kanban.boardmanagement.usecases.queries.viewcardhistory;

import java.util.Optional;

/** 讀取模型的唯讀 Gateway：卡片移動歷程。實作在外圈（CA-6）。 */
public interface CardHistoryReader {

    Optional<CardHistory> cardHistory(String boardId, String cardId);
}
