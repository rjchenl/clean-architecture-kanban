package kanban.flowanalysis.adapters.projection;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 統計方式 S1（設計第 7.4 節）：每週從週一 00:00 起算，以 Main 傳入的時區計。全系統唯一一份實作。
 * 只決定怎麼把已經判斷好的結果分組，不決定任何一個工作項目算不算完成（設計第 0 節 4c）。
 */
public final class WeekCalendar {

    private final ZoneId zone;

    public WeekCalendar(ZoneId zone) {
        this.zone = zone;
    }

    /** t 所在那一週的週一 00:00。 */
    public Instant weekStartOf(Instant t) {
        return mondayOf(t).atStartOfDay(zone).toInstant();
    }

    /** 最近 count 週的起點，含本週，由舊到新。 */
    public List<Instant> recentWeekStarts(Instant asOf, int count) {
        LocalDate thisMonday = mondayOf(asOf);
        return IntStream.range(0, count)
                .mapToObj(i -> thisMonday.minusWeeks(count - 1L - i).atStartOfDay(zone).toInstant())
                .toList();
    }

    private LocalDate mondayOf(Instant t) {
        return t.atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
