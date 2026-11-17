package kanban.flowanalysis.adapters.projection;

import java.util.List;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.ColumnEntry;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.WorkItemEntry;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ColumnMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.Delivery;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.StuckWorkItem;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.WeeklyCompletion;

/**
 * 統計方式 S2 裡「從看板流程文件算的那一半」（各欄張數、停留平均、卡住清單），記憶體版與 PostgreSQL 版共用，
 * 兩邊算法一致。平均四捨五入到整數秒。每週完成數與交付時間由各自的儲存查出來後交給這裡組起來。
 */
final class FlowMetricsAssembler {

    private FlowMetricsAssembler() {
    }

    static FlowMetrics assemble(FlowBoardDocument doc, List<WeeklyCompletion> weeks, Delivery delivery) {
        List<ColumnMetrics> columns = doc.columns().stream()
                .filter(column -> !column.removed())
                .map(column -> new ColumnMetrics(column.flowColumnId(), column.name(), column.stage(),
                        (int) doc.workItems().stream().filter(item -> item.flowColumnId().equals(column.flowColumnId())).count(),
                        column.dwellCount() == 0 ? null
                                : Math.round((double) column.dwellSecondsTotal() / column.dwellCount())))
                .toList();
        List<StuckWorkItem> stuck = doc.workItems().stream()
                .filter(WorkItemEntry::stuck)
                .map(item -> new StuckWorkItem(item.workItemId(), item.title(), columnName(doc, item.flowColumnId()),
                        item.stuckSince()))
                .toList();
        return new FlowMetrics(doc.boardFlowId(), doc.name(), doc.archived(), doc.stuckThresholdDays(), columns, weeks,
                delivery, stuck);
    }

    private static String columnName(FlowBoardDocument doc, String columnId) {
        return doc.columns().stream().filter(column -> column.flowColumnId().equals(columnId))
                .map(ColumnEntry::name).findFirst().orElse("");
    }
}
