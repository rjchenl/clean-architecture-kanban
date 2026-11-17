package kanban.flowanalysis.usecases.commands;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import kanban.flowanalysis.entities.boardflow.ActivityPosition;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import org.junit.jupiter.api.Test;

/**
 * DDD-5 的行為檢查本身會咬人：一次指令存了兩個看板流程，FakeBoardFlowRepository 的檢查必須失敗。
 * 每個指令 Use Case 測試（繼承 FlowCommandFixture）結束時都會自動跑這個檢查；這支測試刻意不繼承它，才能直接驗檢查本身。
 */
class OneFlowPerCommandCheckTest {

    private static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");
    private static final BoardFlowId FLOW_A = new BoardFlowId("board-a");
    private static final BoardFlowId FLOW_B = new BoardFlowId("board-b");

    private final FakeBoardFlowRepository flows = new FakeBoardFlowRepository();

    @Test
    void 一次指令存了兩個看板流程_檢查失敗() {
        givenFlows(flows);

        // 違規樣本：同一次指令載入並存回兩個看板流程
        flows.save(flows.load(FLOW_A).orElseThrow());
        flows.save(flows.load(FLOW_B).orElseThrow());

        assertThatThrownBy(flows::assertAtMostOneFlowSaved)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("DDD-5");
    }

    @Test
    void 兩次指令各存一個看板流程_檢查通過() {
        givenFlows(flows);

        flows.save(flows.load(FLOW_A).orElseThrow());
        flows.commandFinished();
        flows.save(flows.load(FLOW_B).orElseThrow());

        assertThatCode(flows::commandFinished).doesNotThrowAnyException();
    }

    @Test
    void 指令測試的共用場景在收到回覆時自動檢查() {
        FlowCommandFixture commandTest = new FlowCommandFixture() {
        };
        givenFlows(commandTest.flows);

        // 違規樣本：一個指令存了兩個看板流程之後才回覆
        commandTest.flows.save(commandTest.flows.load(FLOW_A).orElseThrow());
        commandTest.flows.save(commandTest.flows.load(FLOW_B).orElseThrow());

        assertThatThrownBy(() -> commandTest.responses.add(CommandResponse.success(null)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("DDD-5");
    }

    private static void givenFlows(FakeBoardFlowRepository repository) {
        repository.given(FLOW_A, List.of(new BoardFlowCreated(FLOW_A, new ActivityPosition(1), new FlowBoardName("甲"), T0)));
        repository.given(FLOW_B, List.of(new BoardFlowCreated(FLOW_B, new ActivityPosition(1), new FlowBoardName("乙"), T0)));
    }
}
