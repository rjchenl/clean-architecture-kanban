package kanban.flowanalysis.entities.boardflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class IdentifiersTest {

    private static void assertRejected(ThrowingCallable creation, RejectionReason expected) {
        assertThatThrownBy(creation)
                .isInstanceOfSatisfying(BusinessRuleViolation.class,
                        violation -> assertThat(violation.reason()).isEqualTo(expected));
    }

    @Test
    void 識別碼空白_拒絕IDENTIFIER_BLANK() {
        assertRejected(() -> new BoardFlowId("  "), RejectionReason.IDENTIFIER_BLANK);
        assertRejected(() -> new FlowColumnId(""), RejectionReason.IDENTIFIER_BLANK);
        assertRejected(() -> new WorkItemId(null), RejectionReason.IDENTIFIER_BLANK);
    }

    @Test
    void 識別碼含控制字元_拒絕INVALID_CHARACTERS() {
        assertRejected(() -> new WorkItemId("a\u0000b"), RejectionReason.INVALID_CHARACTERS);
        assertRejected(() -> new BoardFlowId("board\n1"), RejectionReason.INVALID_CHARACTERS);
    }

    @Test
    void 相同值的識別碼相等() {
        assertThat(new FlowColumnId("col-1")).isEqualTo(new FlowColumnId("col-1"));
        assertThat(new BoardFlowId("board-1").value()).isEqualTo("board-1");
    }
}
