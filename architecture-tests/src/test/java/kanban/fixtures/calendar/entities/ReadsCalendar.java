package kanban.fixtures.calendar.entities;

import java.util.Calendar;

/** 違規樣本（ES-4）：用 Calendar.getInstance() 讀系統時間。 */
public final class ReadsCalendar {

    Object now() {
        return Calendar.getInstance();
    }
}
