package kanban.fixtures.instantsource.entities;

import java.time.Instant;
import java.time.InstantSource;

/** 違規樣本（ES-4）：用 InstantSource.system() 讀系統時間（不經過 Clock，也不叫 now()）。 */
public final class ReadsInstantSource {

    Instant now() {
        return InstantSource.system().instant();
    }
}
