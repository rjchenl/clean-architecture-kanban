package kanban.boardmanagement.adapters.system;

import java.time.Instant;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;

/** 用系統時鐘當時間來源。 */
public final class SystemTimeSource implements TimeSource {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
