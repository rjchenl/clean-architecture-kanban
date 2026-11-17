package kanban.flowanalysis.adapters.eventstore;

/** 事件紀錄裡有這一版程式不認得的事件類型或格式版本：明確報錯，NEVER 默默略過。 */
public final class UnknownEventTypeException extends RuntimeException {

    public UnknownEventTypeException(String eventType, int schemaVersion) {
        super("這一版程式不認得事件 " + eventType + " 的格式版本 " + schemaVersion);
    }
}
