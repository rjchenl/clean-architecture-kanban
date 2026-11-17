package kanban.flowanalysis.adapters.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kanban.flowanalysis.usecases.commands.checkstuckworkitems.CheckStuckWorkItemsRequest;
import kanban.flowanalysis.usecases.commands.removestuckthreshold.RemoveStuckThresholdRequest;
import kanban.flowanalysis.usecases.commands.setcolumnstage.SetColumnStageRequest;
import kanban.flowanalysis.usecases.commands.setstuckthreshold.SetStuckThresholdRequest;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsRequest;
import org.junit.jupiter.api.Test;

/** Controller 只把收到的值組成 Request Model 交給 Input Boundary，不做判斷（CA-4）。 */
class ControllersTest {

    private final List<Object> requests = new ArrayList<>();

    @Test
    void 指定欄位階段() {
        new SetColumnStageController(requests::add).handle("board-1", "col-doing", "IN_PROGRESS");

        assertThat(requests).containsExactly(new SetColumnStageRequest("board-1", "col-doing", "IN_PROGRESS"));
    }

    @Test
    void 設定與取消卡住門檻() {
        new SetStuckThresholdController(requests::add).handle("board-1", 5);
        new RemoveStuckThresholdController(requests::add).handle("board-1");

        assertThat(requests).containsExactly(new SetStuckThresholdRequest("board-1", 5),
                new RemoveStuckThresholdRequest("board-1"));
    }

    @Test
    void 檢查卡住的工作項目() {
        new CheckStuckWorkItemsController(requests::add).handle("board-1");

        assertThat(requests).containsExactly(new CheckStuckWorkItemsRequest("board-1"));
    }

    @Test
    void 查看流程數字_帶入現在的時間() {
        Instant now = Instant.parse("2026-11-18T04:00:00Z");

        new ViewFlowMetricsController(requests::add).handle("board-1", now);

        assertThat(requests).containsExactly(new ViewFlowMetricsRequest("board-1", now));
    }

    @Test
    void 看板流程清單() {
        List<String> called = new ArrayList<>();

        new ListBoardFlowsController(() -> called.add("listed")).handle();

        assertThat(called).containsExactly("listed");
    }
}
