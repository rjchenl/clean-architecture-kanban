package kanban.flowanalysis.usecases.commands.checkstuckworkitems;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import kanban.flowanalysis.entities.boardflow.DwellTime;
import kanban.flowanalysis.entities.boardflow.StuckThreshold;
import kanban.flowanalysis.entities.boardflow.StuckThresholdSet;
import kanban.flowanalysis.entities.boardflow.WorkItemFlowResultChanged;
import kanban.flowanalysis.entities.boardflow.WorkItemMoved;
import kanban.flowanalysis.entities.boardflow.WorkItemStuckStatusChanged;
import kanban.flowanalysis.usecases.commands.CommandResponse;
import kanban.flowanalysis.usecases.commands.FlowCommandFixture;
import kanban.flowanalysis.usecases.commands.checkstuckworkitems.impl.CheckStuckWorkItemsUseCase;
import org.junit.jupiter.api.Test;

class CheckStuckWorkItemsUseCaseTest extends FlowCommandFixture {

    private final CheckStuckWorkItemsInput useCase = new CheckStuckWorkItemsUseCase(flows, time, responses::add);

    @Test
    void 用現在的時間判斷_待滿門檻的標成卡住() {
        // card-1 從 T1 起在進行中，門檻 1 天；NOW 已經過了 4 天
        givenStandardFlow(
                new WorkItemMoved(FLOW, at(5), CARD_1, TODO, DOING, new DwellTime(Duration.ofHours(1)), T1),
                new WorkItemFlowResultChanged(FLOW, CARD_1, T1, null, null, T1),
                new StuckThresholdSet(FLOW, new StuckThreshold(1), T1));

        useCase.execute(new CheckStuckWorkItemsRequest("board-1"));

        assertSucceeded(null);
        assertThat(newEvents()).containsExactly(new WorkItemStuckStatusChanged(FLOW, CARD_1, true, DOING, T1, NOW));
    }

    @Test
    void 沒有門檻_成功且沒有新事件() {
        givenStandardFlow();

        useCase.execute(new CheckStuckWorkItemsRequest("board-1"));

        assertSucceeded(null);
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 看板流程不存在_回覆BOARD_FLOW_NOT_FOUND() {
        useCase.execute(new CheckStuckWorkItemsRequest("board-1"));

        assertFailed(CommandResponse.BOARD_FLOW_NOT_FOUND);
    }
}
