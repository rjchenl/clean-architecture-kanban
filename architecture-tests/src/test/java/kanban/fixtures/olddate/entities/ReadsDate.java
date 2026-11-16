package kanban.fixtures.olddate.entities;

import java.util.Date;

/** 違規樣本（ES-4）：用 new Date() 讀系統時間。 */
public final class ReadsDate {

    Object now() {
        return new Date();
    }
}
