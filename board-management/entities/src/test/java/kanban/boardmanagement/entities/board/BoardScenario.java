package kanban.boardmanagement.entities.board;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 聚合測試的統一寫法（ES-5）：Given 已發生的事件／When 指令／Then 產生的事件或被拒絕。
 * 聚合測試只能透過這個工具操作聚合，不直接重播事件或讀未提交事件。
 */
public final class BoardScenario {

    public static final BoardId BOARD = new BoardId("board-1");
    public static final Instant T0 = Instant.parse("2026-11-14T01:00:00Z");
    public static final Instant T1 = T0.plusSeconds(60);

    private BoardScenario() {
    }

    /** Given：這些事件已經發生。 */
    public static Given given(BoardEvent... history) {
        return new Given(List.of(history));
    }

    /** 用於「建立看板」這種沒有既有歷史的指令。 */
    public static Then whenCreating(Supplier<Board> creation) {
        try {
            return new Then(creation.get().uncommittedEvents(), null);
        } catch (BusinessRuleViolation violation) {
            return new Then(List.of(), violation);
        }
    }

    public static final class Given {

        private final List<BoardEvent> history;

        private Given(List<BoardEvent> history) {
            this.history = history;
        }

        /** When：對重播出來的看板下一個指令。 */
        public Then when(Consumer<Board> command) {
            Board board = Board.rehydrate(history);
            try {
                command.accept(board);
                return new Then(board.uncommittedEvents(), null);
            } catch (BusinessRuleViolation violation) {
                return new Then(board.uncommittedEvents(), violation);
            }
        }
    }

    public static final class Then {

        private final List<BoardEvent> produced;
        private final BusinessRuleViolation violation;

        private Then(List<BoardEvent> produced, BusinessRuleViolation violation) {
            this.produced = produced;
            this.violation = violation;
        }

        /** Then：恰好產生這些事件。 */
        public void thenEvents(BoardEvent... expected) {
            assertThat(violation).as("指令不應該被拒絕").isNull();
            assertThat(produced).containsExactly(expected);
        }

        /** Then：指令成功，但沒有任何事件。 */
        public void thenNoEvents() {
            assertThat(violation).as("指令不應該被拒絕").isNull();
            assertThat(produced).isEmpty();
        }

        /** Then：指令被拒絕，而且沒有留下任何事件。 */
        public void thenRejected(RejectionReason reason) {
            assertThat(violation).as("指令應該被拒絕：%s", reason).isNotNull();
            assertThat(violation.reason()).isEqualTo(reason);
            assertThat(produced).as("被拒絕的指令不可以留下事件").isEmpty();
        }
    }
}
