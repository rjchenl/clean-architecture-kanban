package kanban.fixtures.gatewayunnamed.usecases.commands.gateway;

/** Use Cases 圈的 Gateway 介面，名稱沒照 Repository／Reader 的慣例。 */
public interface ThingStore {
    void save(String thing);
}
