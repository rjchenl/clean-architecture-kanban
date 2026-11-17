package kanban.flowanalysis.adapters.system;

import java.time.Instant;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;

/** 系統時鐘。 */
public final class SystemTimeSource implements TimeSource {

    @Override
    public Instant now() {
        return Instant.now();
    }
}
