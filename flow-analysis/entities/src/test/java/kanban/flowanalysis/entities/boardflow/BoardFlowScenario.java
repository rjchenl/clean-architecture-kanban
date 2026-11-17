package kanban.flowanalysis.entities.boardflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 看板流程聚合測試的統一寫法（ES-5）：Given 已發生的事件／When 指令／Then 產生的事件、被拒絕或程式錯誤。
 * 聚合測試只能透過這個工具操作聚合，不直接重播事件或讀未提交事件。
 */
public final class BoardFlowScenario {

    public static final BoardFlowId FLOW = new BoardFlowId("board-1");
    public static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");

    private BoardFlowScenario() {
    }

    /** 看板動態位置。 */
    public static ActivityPosition at(long position) {
        return new ActivityPosition(position);
    }

    /** T0 之後 n 天。 */
    public static Instant days(double n) {
        return T0.plus(Duration.ofSeconds(Math.round(n * 86_400)));
    }

    /** Given：這些事件已經發生。 */
    public static Given given(BoardFlowEvent... history) {
        return new Given(List.of(history));
    }

    /** 用於「建立看板流程」這種沒有既有歷史的動作。 */
    public static Then whenCreating(Supplier<BoardFlow> creation) {
        try {
            return new Then(creation.get().uncommittedEvents(), null, null);
        } catch (BusinessRuleViolation violation) {
            return new Then(List.of(), violation, null);
        }
    }

    public static final class Given {

        private final List<BoardFlowEvent> history;

        private Given(List<BoardFlowEvent> history) {
            this.history = history;
        }

        /** When：對重播出來的看板流程下一個指令，或交給它一件看板上發生的事。 */
        public Then when(Consumer<BoardFlow> command) {
            BoardFlow flow = BoardFlow.rehydrate(history);
            try {
                command.accept(flow);
                return new Then(flow.uncommittedEvents(), null, null);
            } catch (BusinessRuleViolation violation) {
                return new Then(flow.uncommittedEvents(), violation, null);
            } catch (IllegalStateException | IllegalArgumentException failure) {
                return new Then(flow.uncommittedEvents(), null, failure);
            }
        }
    }

    public static final class Then {

        private final List<BoardFlowEvent> produced;
        private final BusinessRuleViolation violation;
        private final RuntimeException failure;

        private Then(List<BoardFlowEvent> produced, BusinessRuleViolation violation, RuntimeException failure) {
            this.produced = produced;
            this.violation = violation;
            this.failure = failure;
        }

        /** Then：恰好產生這些事件。 */
        public void thenEvents(BoardFlowEvent... expected) {
            assertSucceeded();
            assertThat(produced).containsExactly(expected);
        }

        /** Then：成功，但沒有任何事件。 */
        public void thenNoEvents() {
            assertSucceeded();
            assertThat(produced).isEmpty();
        }

        /** Then：使用者的指令被拒絕，而且沒有留下任何事件。 */
        public void thenRejected(RejectionReason reason) {
            assertThat(failure).as("不應該是程式錯誤").isNull();
            assertThat(violation).as("指令應該被拒絕：%s", reason).isNotNull();
            assertThat(violation.reason()).isEqualTo(reason);
            assertThat(produced).as("被拒絕的指令不可以留下事件").isEmpty();
        }

        /** Then：看板上的事實和看板流程記錄的對不上（只有程式錯誤才會發生），丟例外而且沒有留下任何事件。 */
        public void thenFails(Class<? extends RuntimeException> type) {
            assertThat(violation).as("看板上的事實不會被業務規則拒絕（F14）").isNull();
            assertThat(failure).as("應該丟出 %s", type.getSimpleName()).isInstanceOf(type);
            assertThat(produced).as("失敗時不可以留下事件").isEmpty();
        }

        /** 產生的事件，給需要比對整串結果的測試用（例如重算的等價測試）。 */
        public List<BoardFlowEvent> events() {
            assertSucceeded();
            return produced;
        }

        private void assertSucceeded() {
            assertThat(violation).as("不應該被拒絕").isNull();
            assertThat(failure).as("不應該失敗").isNull();
        }
    }
}
