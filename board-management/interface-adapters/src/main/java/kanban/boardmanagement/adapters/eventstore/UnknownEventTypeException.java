package kanban.boardmanagement.adapters.eventstore;

/** 事件紀錄裡出現這一版程式不認得的事件類型或格式版本。必須明確報錯，不可以默默略過。 */
public final class UnknownEventTypeException extends RuntimeException {

    public UnknownEventTypeException(String eventType, int schemaVersion) {
        super("這一版程式不認得事件 " + eventType + " 的格式版本 " + schemaVersion);
    }
}
