package kanban.flowanalysis.entities.boardflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {

    private static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");
    private static final Instant T1 = T0.plus(Duration.ofHours(36));

    private static void assertRejected(ThrowingCallable creation, RejectionReason expected) {
        assertThatThrownBy(creation)
                .isInstanceOfSatisfying(BusinessRuleViolation.class,
                        violation -> assertThat(violation.reason()).isEqualTo(expected));
    }

    @Test
    void 階段依名稱取得() {
        assertThat(Stage.named("IN_PROGRESS")).isEqualTo(Stage.IN_PROGRESS);
        assertThat(Stage.named("UNCLASSIFIED")).isEqualTo(Stage.UNCLASSIFIED);
    }

    @Test
    void 不認得的階段_拒絕UNKNOWN_STAGE() {
        assertRejected(() -> Stage.named("doing"), RejectionReason.UNKNOWN_STAGE);
        assertRejected(() -> Stage.named(null), RejectionReason.UNKNOWN_STAGE);
    }

    @Test
    void 卡住門檻是1到365天() {
        assertThat(new StuckThreshold(1).length()).isEqualTo(Duration.ofDays(1));
        assertThat(new StuckThreshold(365).days()).isEqualTo(365);
        assertRejected(() -> new StuckThreshold(0), RejectionReason.STUCK_THRESHOLD_OUT_OF_RANGE);
        assertRejected(() -> new StuckThreshold(366), RejectionReason.STUCK_THRESHOLD_OUT_OF_RANGE);
    }

    @Test
    void 看板動態位置從1起算_能比先後() {
        assertThat(new ActivityPosition(5).isAfter(new ActivityPosition(4))).isTrue();
        assertThat(new ActivityPosition(5).isAfter(new ActivityPosition(5))).isFalse();
        assertThatThrownBy(() -> new ActivityPosition(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 名稱類值物件不驗長度_看板上的事一律接受() {
        // F14：看板管理已經驗過；流程分析不另外拒絕，即使看板管理之後放寬長度限制
        String long101 = "字".repeat(101);
        assertThat(new WorkItemTitle(long101).value()).isEqualTo(long101);
        assertThat(new FlowBoardName("  產品開發 ").value()).isEqualTo("產品開發");
        assertThat(new FlowColumnName("待辦").value()).isEqualTo("待辦");
        assertThatThrownBy(() -> new WorkItemTitle(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 流程欄位位置從0起算() {
        assertThat(new FlowColumnPosition(0).index()).isZero();
        assertThatThrownBy(() -> new FlowColumnPosition(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 停留時間與交付時間是兩個時間點的差() {
        assertThat(DwellTime.between(T0, T1).value()).isEqualTo(Duration.ofHours(36));
        assertThat(LeadTime.between(T0, T1).value()).isEqualTo(Duration.ofHours(36));
    }

    @Test
    void 看板上的時間倒退_停留時間與交付時間以0計() {
        // F15 補充、Review Focus 2：時鐘回撥時不丟例外，免得同步停住
        assertThat(DwellTime.between(T1, T0).value()).isEqualTo(Duration.ZERO);
        assertThat(LeadTime.between(T1, T0).value()).isEqualTo(Duration.ZERO);
    }

    @Test
    void 時間長度不可為負數或空白() {
        assertThatThrownBy(() -> new LeadTime(Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DwellTime(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
