package kanban.fixtures.gatewayunnamed.frameworks.persistence;

import kanban.fixtures.gatewayunnamed.usecases.commands.gateway.ThingStore;

/** 違規樣本（DDD-8、CA-6）：Gateway 的實作放在最外圈，而不是 Interface Adapters 圈。 */
public final class JdbcThingStore implements ThingStore {
    @Override
    public void save(String thing) {
    }
}
