package kanban.flowanalysis.entities.boardflow;

import java.time.Duration;
import java.time.Instant;

/** 停留時間：在某一欄從進入到離開的時間（F7）。被刪除不算離開。 */
public record DwellTime(Duration value) {

    public DwellTime {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException("停留時間不可以是空的或負的：" + value);
        }
    }

    /** 看板上的時間倒退（離開比進入還早）時以 0 計（F15 補充）。 */
    public static DwellTime between(Instant entered, Instant left) {
        return new DwellTime(left.isBefore(entered) ? Duration.ZERO : Duration.between(entered, left));
    }
}
