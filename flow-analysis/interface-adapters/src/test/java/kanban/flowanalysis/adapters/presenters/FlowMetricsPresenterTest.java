package kanban.flowanalysis.adapters.presenters;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ColumnMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.Delivery;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.StuckWorkItem;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsResponse;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.WeeklyCompletion;
import org.junit.jupiter.api.Test;

/** 流程數字整理成可直接顯示的文字（CA-9）：時間、天數、張數、階段的中文都在這裡，畫面不自己整理。 */
class FlowMetricsPresenterTest {

    private static final long DAY = 86_400;

    private final FlowMetricsPresenter presenter = new FlowMetricsPresenter(ZoneId.of("Asia/Taipei"));

    private FlowMetricsViewModel present(FlowMetrics metrics) {
        presenter.present(new ViewFlowMetricsResponse(true, metrics));
        return presenter.viewModel();
    }

    private static FlowMetrics metrics(Integer threshold, List<ColumnMetrics> columns, Delivery delivery,
            List<StuckWorkItem> stuck) {
        return new FlowMetrics("board-1", "產品開發", false, threshold, columns,
                List.of(new WeeklyCompletion(Instant.parse("2026-10-25T16:00:00Z"), 2),
                        new WeeklyCompletion(Instant.parse("2026-11-01T16:00:00Z"), 0)),
                delivery, stuck);
    }

    @Test
    void 各欄_階段中文_張數_平均停留到小數一位() {
        FlowMetricsViewModel view = present(metrics(null, List.of(
                new ColumnMetrics("col-doing", "進行中", "IN_PROGRESS", 2, 3 * DAY + DAY / 2),
                new ColumnMetrics("col-done", "完成", "DONE", 9, null)), new Delivery(0, null, null), List.of()));

        assertThat(view.columns()).containsExactly(
                new FlowColumnViewModel("col-doing", "進行中", "IN_PROGRESS", "進行中", "2", "3.5 天"),
                new FlowColumnViewModel("col-done", "完成", "DONE", "完成", "9", "—"));
    }

    @Test
    void 天數四捨五入到小數一位() {
        // 4.25 天 → 4.3 天；0.04 天 → 0.0 天
        FlowMetricsViewModel view = present(metrics(null, List.of(
                new ColumnMetrics("a", "甲", "UNCLASSIFIED", 0, 4 * DAY + DAY / 4),
                new ColumnMetrics("b", "乙", "NOT_STARTED", 0, 3_456L)), new Delivery(0, null, null), List.of()));

        assertThat(view.columns()).extracting(FlowColumnViewModel::averageDwell).containsExactly("4.3 天", "0.0 天");
        assertThat(view.columns()).extracting(FlowColumnViewModel::stageLabel).containsExactly("未分類", "未開始");
    }

    @Test
    void 交付摘要與每週完成數() {
        FlowMetricsViewModel view = present(metrics(null, List.of(), new Delivery(12, 4 * DAY + DAY / 5, 9 * DAY), List.of()));

        assertThat(view.deliverySummary()).isEqualTo("最近 8 週：完成 12 張，交付時間平均 4.2 天、最長 9.0 天");
        // 週的起點以台北時間顯示：2026-10-25T16:00Z 是台北的 10/26（週一）00:00
        assertThat(view.weeks()).containsExactly(new WeekViewModel("10/26", "2 張"), new WeekViewModel("11/02", "0 張"));
    }

    @Test
    void 沒有完成的工作項目() {
        assertThat(present(metrics(null, List.of(), new Delivery(0, null, null), List.of())).deliverySummary())
                .isEqualTo("最近 8 週沒有完成的工作項目");
    }

    @Test
    void 卡住清單_起點以台北時間顯示() {
        FlowMetricsViewModel view = present(metrics(5, List.of(), new Delivery(0, null, null),
                List.of(new StuckWorkItem("card-1", "串接金流", "進行中", Instant.parse("2026-11-08T06:20:00Z")))));

        assertThat(view.stuckWorkItems()).containsExactly(new StuckWorkItemViewModel("串接金流", "進行中", "11/08 14:20"));
    }

    @Test
    void 卡住門檻_有設與沒設() {
        FlowMetricsViewModel set = present(metrics(5, List.of(), new Delivery(0, null, null), List.of()));
        assertThat(set.hasStuckThreshold()).isTrue();
        assertThat(set.stuckThresholdDays()).isEqualTo("5");
        assertThat(set.stuckThresholdLabel()).isEqualTo("卡住門檻 5 天");

        FlowMetricsViewModel unset = present(metrics(null, List.of(), new Delivery(0, null, null), List.of()));
        assertThat(unset.hasStuckThreshold()).isFalse();
        assertThat(unset.stuckThresholdDays()).isEmpty();
        assertThat(unset.stuckThresholdLabel()).isEqualTo("沒有設定卡住門檻");
    }

    @Test
    void 階段選項依順序列出四個() {
        assertThat(present(metrics(null, List.of(), new Delivery(0, null, null), List.of())).stageOptions()).containsExactly(
                new StageOptionViewModel("UNCLASSIFIED", "未分類"),
                new StageOptionViewModel("NOT_STARTED", "未開始"),
                new StageOptionViewModel("IN_PROGRESS", "進行中"),
                new StageOptionViewModel("DONE", "完成"));
    }

    @Test
    void 封存的看板_標示已封存() {
        presenter.present(new ViewFlowMetricsResponse(true, new FlowMetrics("board-1", "產品開發", true, null,
                List.of(), List.of(), new Delivery(0, null, null), List.of())));

        assertThat(presenter.viewModel().archivedLabel()).isEqualTo("已封存");
    }

    @Test
    void 找不到_顯示還沒準備好() {
        // Review Focus 3：看板剛建立、還沒同步過來
        presenter.present(new ViewFlowMetricsResponse(false, null));

        FlowMetricsViewModel view = presenter.viewModel();
        assertThat(view.found()).isFalse();
        assertThat(view.notFoundMessage()).isEqualTo("流程分析還沒準備好，請稍後重新整理");
        assertThat(view.columns()).isEmpty();
        assertThat(view.weeks()).isEmpty();
        assertThat(view.stuckWorkItems()).isEmpty();
    }
}
