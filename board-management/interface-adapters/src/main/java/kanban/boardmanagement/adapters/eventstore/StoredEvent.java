package kanban.boardmanagement.adapters.eventstore;

/** 一筆事件存進事件紀錄時的樣子：類型名稱、格式版本、JSON 內容。 */
public record StoredEvent(String eventType, int schemaVersion, String payloadJson) {
}
