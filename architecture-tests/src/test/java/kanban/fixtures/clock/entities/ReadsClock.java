package kanban.fixtures.clock.entities;

import java.time.Instant;

/** 違規樣本（ES-4）：領域模型自己讀系統時間。 */
public record ReadsClock(String value) {

    public Instant stamp() {
        return Instant.now();
    }
}
