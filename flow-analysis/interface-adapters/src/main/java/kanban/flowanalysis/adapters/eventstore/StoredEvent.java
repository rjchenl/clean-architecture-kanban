package kanban.flowanalysis.adapters.eventstore;

/** 事件存進事件紀錄時的樣子：事件類型名稱、格式版本、JSON 內容（ES-7）。 */
public record StoredEvent(String eventType, int schemaVersion, String payloadJson) {
}
