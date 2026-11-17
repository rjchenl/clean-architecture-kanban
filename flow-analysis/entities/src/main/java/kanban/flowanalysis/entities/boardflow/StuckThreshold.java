package kanban.flowanalysis.entities.boardflow;

import java.time.Duration;

/** 卡住門檻：在進行中的同一欄待幾天算卡住。1～365 的整數天數（F8）。 */
public record StuckThreshold(int days) {

    static final int MIN_DAYS = 1;
    static final int MAX_DAYS = 365;

    public StuckThreshold {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new BusinessRuleViolation(RejectionReason.STUCK_THRESHOLD_OUT_OF_RANGE);
        }
    }

    public Duration length() {
        return Duration.ofDays(days);
    }
}
