package kanban.boardmanagement.usecases.queries.viewcardhistory;

import java.time.Instant;

/** 讀取模型：卡片移動歷程的一筆。欄位名稱是事件發生當時的名稱。ADDED 時 fromColumnName 為 null；REMOVED 時 toColumnName 為 null。 */
public record CardHistoryEntry(CardHistoryEntryKind kind, String fromColumnName, String toColumnName, Instant occurredAt) {
}
