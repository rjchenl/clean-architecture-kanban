package kanban.flowanalysis.adapters.presenters;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.Delivery;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsOutput;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ViewFlowMetricsResponse;

/**
 * 把流程數字整理成可直接顯示的文字（CA-9）：時間轉成當地時間、秒數轉成「3.5 天」、階段轉成中文。
 * 只整理格式，不判斷算不算完成或卡住（那些都由看板流程判斷好了）。
 */
public final class FlowMetricsPresenter implements ViewFlowMetricsOutput {

    static final String NOT_READY = "流程分析還沒準備好，請稍後重新整理";
    private static final BigDecimal SECONDS_PER_DAY = BigDecimal.valueOf(86_400);
    private static final List<String> STAGES = List.of("UNCLASSIFIED", "NOT_STARTED", "IN_PROGRESS", "DONE");
    private static final Map<String, String> STAGE_LABELS = Map.of(
            "UNCLASSIFIED", "未分類", "NOT_STARTED", "未開始", "IN_PROGRESS", "進行中", "DONE", "完成");
    private static final List<StageOptionViewModel> STAGE_OPTIONS = STAGES.stream()
            .map(stage -> new StageOptionViewModel(stage, STAGE_LABELS.get(stage)))
            .toList();

    private final DateTimeFormatter weekFormat;
    private final DateTimeFormatter momentFormat;
    private FlowMetricsViewModel viewModel;

    public FlowMetricsPresenter(ZoneId zone) {
        this.weekFormat = DateTimeFormatter.ofPattern("MM/dd").withZone(zone);
        this.momentFormat = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(zone);
    }

    @Override
    public void present(ViewFlowMetricsResponse response) {
        if (!response.found()) {
            viewModel = new FlowMetricsViewModel(false, "", "", "", false, "", "", STAGE_OPTIONS, List.of(), "",
                    List.of(), List.of(), NOT_READY);
            return;
        }
        FlowMetrics metrics = response.metrics();
        Integer threshold = metrics.stuckThresholdDays();
        viewModel = new FlowMetricsViewModel(true, metrics.boardFlowId(), metrics.boardName(),
                metrics.archived() ? "已封存" : "",
                threshold != null,
                threshold == null ? "" : String.valueOf(threshold),
                threshold == null ? "沒有設定卡住門檻" : "卡住門檻 " + threshold + " 天",
                STAGE_OPTIONS,
                metrics.columns().stream()
                        .map(column -> new FlowColumnViewModel(column.flowColumnId(), column.name(), column.stage(),
                                STAGE_LABELS.getOrDefault(column.stage(), column.stage()),
                                String.valueOf(column.workItemCount()), days(column.averageDwellSeconds())))
                        .toList(),
                deliverySummary(metrics.delivery()),
                metrics.recentWeeks().stream()
                        .map(week -> new WeekViewModel(weekFormat.format(week.weekStartsAt()), week.completedCount() + " 張"))
                        .toList(),
                metrics.stuckWorkItems().stream()
                        .map(item -> new StuckWorkItemViewModel(item.title(), item.flowColumnName(),
                                momentFormat.format(item.stuckSince())))
                        .toList(),
                "");
    }

    public FlowMetricsViewModel viewModel() {
        if (viewModel == null) {
            throw new IllegalStateException("Use Case 還沒有交出結果");
        }
        return viewModel;
    }

    private static String deliverySummary(Delivery delivery) {
        if (delivery.completedCount() == 0) {
            return "最近 8 週沒有完成的工作項目";
        }
        return "最近 8 週：完成 " + delivery.completedCount() + " 張，交付時間平均 "
                + days(delivery.averageLeadTimeSeconds()) + "、最長 " + days(delivery.longestLeadTimeSeconds());
    }

    /** 秒數 → 「3.5 天」，四捨五入到小數一位；沒有值顯示「—」。 */
    private static String days(Long seconds) {
        if (seconds == null) {
            return "—";
        }
        return BigDecimal.valueOf(seconds).divide(SECONDS_PER_DAY, 1, RoundingMode.HALF_UP).toPlainString() + " 天";
    }
}
