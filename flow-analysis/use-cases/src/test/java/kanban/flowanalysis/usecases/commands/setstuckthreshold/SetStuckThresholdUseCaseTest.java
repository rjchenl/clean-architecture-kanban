package kanban.flowanalysis.usecases.commands.setstuckthreshold;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.flowanalysis.entities.boardflow.RejectionReason;
import kanban.flowanalysis.entities.boardflow.StuckThreshold;
import kanban.flowanalysis.entities.boardflow.StuckThresholdSet;
import kanban.flowanalysis.usecases.commands.CommandResponse;
import kanban.flowanalysis.usecases.commands.FlowCommandFixture;
import kanban.flowanalysis.usecases.commands.setstuckthreshold.impl.SetStuckThresholdUseCase;
import org.junit.jupiter.api.Test;

class SetStuckThresholdUseCaseTest extends FlowCommandFixture {

    private final SetStuckThresholdInput useCase = new SetStuckThresholdUseCase(flows, time, responses::add);

    @Test
    void 成功_產生StuckThresholdSet() {
        givenStandardFlow();

        useCase.execute(new SetStuckThresholdRequest("board-1", 5));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(new StuckThresholdSet(FLOW, new StuckThreshold(5), NOW));
    }

    @Test
    void 門檻0或366_回覆STUCK_THRESHOLD_OUT_OF_RANGE() {
        givenStandardFlow();

        useCase.execute(new SetStuckThresholdRequest("board-1", 0));
        useCase.execute(new SetStuckThresholdRequest("board-1", 366));

        assertThat(responses).containsOnly(CommandResponse.failure(RejectionReason.STUCK_THRESHOLD_OUT_OF_RANGE.name()))
                .hasSize(2);
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 天數沒給_回覆INVALID_REQUEST() {
        givenStandardFlow();

        useCase.execute(new SetStuckThresholdRequest("board-1", null));

        assertFailed(CommandResponse.INVALID_REQUEST);
    }

    @Test
    void 看板流程不存在_回覆BOARD_FLOW_NOT_FOUND() {
        useCase.execute(new SetStuckThresholdRequest("board-1", 5));

        assertFailed(CommandResponse.BOARD_FLOW_NOT_FOUND);
    }
}
