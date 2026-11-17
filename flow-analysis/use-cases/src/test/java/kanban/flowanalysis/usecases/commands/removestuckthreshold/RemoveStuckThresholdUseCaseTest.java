package kanban.flowanalysis.usecases.commands.removestuckthreshold;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.flowanalysis.entities.boardflow.StuckThreshold;
import kanban.flowanalysis.entities.boardflow.StuckThresholdRemoved;
import kanban.flowanalysis.entities.boardflow.StuckThresholdSet;
import kanban.flowanalysis.usecases.commands.CommandResponse;
import kanban.flowanalysis.usecases.commands.FlowCommandFixture;
import kanban.flowanalysis.usecases.commands.removestuckthreshold.impl.RemoveStuckThresholdUseCase;
import org.junit.jupiter.api.Test;

class RemoveStuckThresholdUseCaseTest extends FlowCommandFixture {

    private final RemoveStuckThresholdInput useCase = new RemoveStuckThresholdUseCase(flows, time, responses::add);

    @Test
    void 成功_產生StuckThresholdRemoved() {
        givenStandardFlow(new StuckThresholdSet(FLOW, new StuckThreshold(5), T0));

        useCase.execute(new RemoveStuckThresholdRequest("board-1"));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(new StuckThresholdRemoved(FLOW, NOW));
    }

    @Test
    void 本來沒有門檻_成功且沒有新事件() {
        givenStandardFlow();

        useCase.execute(new RemoveStuckThresholdRequest("board-1"));

        assertSucceeded(null);
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 看板流程不存在_回覆BOARD_FLOW_NOT_FOUND() {
        useCase.execute(new RemoveStuckThresholdRequest("board-1"));

        assertFailed(CommandResponse.BOARD_FLOW_NOT_FOUND);
    }

    @Test
    void Request本身為null_回覆INVALID_REQUEST() {
        useCase.execute(null);

        assertFailed(CommandResponse.INVALID_REQUEST);
    }
}
