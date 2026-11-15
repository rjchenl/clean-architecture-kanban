package kanban.boardmanagement.adapters.system;

import java.util.UUID;
import kanban.boardmanagement.usecases.commands.gateway.IdentityGenerator;

/** 用隨機 UUID 當識別碼。 */
public final class UuidIdentityGenerator implements IdentityGenerator {

    @Override
    public String nextId() {
        return UUID.randomUUID().toString();
    }
}
