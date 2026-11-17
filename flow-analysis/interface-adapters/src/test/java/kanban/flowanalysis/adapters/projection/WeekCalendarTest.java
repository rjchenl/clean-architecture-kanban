package kanban.flowanalysis.adapters.projection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** 統計方式 S1：每週從週一 00:00 起算，時區由 Main 傳入；最近 8 週含本週。 */
class WeekCalendarTest {

    private final WeekCalendar taipei = new WeekCalendar(ZoneId.of("Asia/Taipei"));

    @Test
    void 週日23點59分屬於上一週_週一0點屬於這一週() {
        // 台北時間 2026-11-15（週日）23:59 與 2026-11-16（週一）00:00
        Instant sundayNight = Instant.parse("2026-11-15T15:59:00Z");
        Instant mondayMidnight = Instant.parse("2026-11-15T16:00:00Z");

        assertThat(taipei.weekStartOf(sundayNight)).isEqualTo(Instant.parse("2026-11-08T16:00:00Z"));
        assertThat(taipei.weekStartOf(mondayMidnight)).isEqualTo(Instant.parse("2026-11-15T16:00:00Z"));
    }

    @Test
    void 同一個時間點_時區不同_週的起點不同() {
        Instant moment = Instant.parse("2026-11-15T20:00:00Z");

        assertThat(taipei.weekStartOf(moment)).isEqualTo(Instant.parse("2026-11-15T16:00:00Z"));
        assertThat(new WeekCalendar(ZoneId.of("UTC")).weekStartOf(moment)).isEqualTo(Instant.parse("2026-11-09T00:00:00Z"));
    }

    @Test
    void 最近8週含本週_由舊到新() {
        // 台北時間 2026-11-18（週三）中午
        assertThat(taipei.recentWeekStarts(Instant.parse("2026-11-18T04:00:00Z"), 8)).containsExactly(
                Instant.parse("2026-09-27T16:00:00Z"),
                Instant.parse("2026-10-04T16:00:00Z"),
                Instant.parse("2026-10-11T16:00:00Z"),
                Instant.parse("2026-10-18T16:00:00Z"),
                Instant.parse("2026-10-25T16:00:00Z"),
                Instant.parse("2026-11-01T16:00:00Z"),
                Instant.parse("2026-11-08T16:00:00Z"),
                Instant.parse("2026-11-15T16:00:00Z"));
    }
}
