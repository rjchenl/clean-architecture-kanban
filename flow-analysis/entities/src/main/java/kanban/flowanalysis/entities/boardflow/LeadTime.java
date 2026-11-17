package kanban.flowanalysis.entities.boardflow;

import java.time.Duration;
import java.time.Instant;

/** 交付時間：完成時間－開始時間（F5）。只有已完成的工作項目才有。 */
public record LeadTime(Duration value) {

    public LeadTime {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException("交付時間不可以是空的或負的：" + value);
        }
    }

    /** 看板上的時間倒退（完成比開始還早）時以 0 計（F15 補充）。 */
    public static LeadTime between(Instant started, Instant completed) {
        return new LeadTime(completed.isBefore(started) ? Duration.ZERO : Duration.between(started, completed));
    }
}
