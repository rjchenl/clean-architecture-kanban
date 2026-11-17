package kanban.flowanalysis.adapters.projection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.ColumnEntry;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.WorkItemEntry;
import kanban.flowanalysis.entities.boardflow.BoardFlowArchived;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.entities.boardflow.BoardFlowRenamed;
import kanban.flowanalysis.entities.boardflow.ColumnStageSet;
import kanban.flowanalysis.entities.boardflow.FlowColumnAdded;
import kanban.flowanalysis.entities.boardflow.FlowColumnMoved;
import kanban.flowanalysis.entities.boardflow.FlowColumnRemoved;
import kanban.flowanalysis.entities.boardflow.FlowColumnRenamed;
import kanban.flowanalysis.entities.boardflow.StuckThresholdRemoved;
import kanban.flowanalysis.entities.boardflow.StuckThresholdSet;
import kanban.flowanalysis.entities.boardflow.WorkItemAdded;
import kanban.flowanalysis.entities.boardflow.WorkItemFlowResultChanged;
import kanban.flowanalysis.entities.boardflow.WorkItemMoved;
import kanban.flowanalysis.entities.boardflow.WorkItemRemoved;
import kanban.flowanalysis.entities.boardflow.WorkItemStuckStatusChanged;
import kanban.flowanalysis.entities.boardflow.WorkItemTitleChanged;

/**
 * 一筆事件套到看板流程文件上（純函式）。只照事件記下的值更新、做加總與計數，沒有任何「算不算」的判斷：
 * 階段、停留時間、卡住與否都由聚合寫在事件裡（DDD-9、設計第 0 節 4c）。
 */
final class FlowBoardProjector {

    private FlowBoardProjector() {
    }

    static FlowBoardDocument apply(FlowBoardDocument current, BoardFlowEvent event) {
        return switch (event) {
            case BoardFlowCreated e -> new FlowBoardDocument(e.boardFlowId().value(), e.name().value(), false, null,
                    List.of(), List.of());
            case BoardFlowRenamed e -> new FlowBoardDocument(current.boardFlowId(), e.name().value(), current.archived(),
                    current.stuckThresholdDays(), current.columns(), current.workItems());
            case BoardFlowArchived e -> new FlowBoardDocument(current.boardFlowId(), current.name(), true,
                    current.stuckThresholdDays(), current.columns(), current.workItems());
            case FlowColumnAdded e -> withColumns(current, ordered(appendedActive(current.columns(),
                    new ColumnEntry(e.flowColumnId().value(), e.name().value(), e.stage().name(), false, 0, 0))));
            case FlowColumnRenamed e -> withColumns(current, replacedColumn(current, e.flowColumnId().value(),
                    column -> new ColumnEntry(column.flowColumnId(), e.name().value(), column.stage(), column.removed(),
                            column.dwellSecondsTotal(), column.dwellCount())));
            case FlowColumnMoved e -> withColumns(current, moved(current.columns(), e.flowColumnId().value(),
                    e.toPosition().index()));
            case FlowColumnRemoved e -> withColumns(current, ordered(replacedColumn(current, e.flowColumnId().value(),
                    column -> new ColumnEntry(column.flowColumnId(), column.name(), column.stage(), true,
                            column.dwellSecondsTotal(), column.dwellCount()))));
            case WorkItemAdded e -> withWorkItems(current, appended(current.workItems(),
                    new WorkItemEntry(e.workItemId().value(), e.title().value(), e.flowColumnId().value(), false, null)));
            case WorkItemTitleChanged e -> withWorkItems(current, replaced(current.workItems(),
                    item -> item.workItemId().equals(e.workItemId().value()),
                    item -> new WorkItemEntry(item.workItemId(), e.title().value(), item.flowColumnId(), item.stuck(),
                            item.stuckSince())));
            case WorkItemMoved e -> movedWorkItem(current, e);
            case WorkItemRemoved e -> withWorkItems(current, current.workItems().stream()
                    .filter(item -> !item.workItemId().equals(e.workItemId().value())).toList());
            case ColumnStageSet e -> withColumns(current, replacedColumn(current, e.flowColumnId().value(),
                    column -> new ColumnEntry(column.flowColumnId(), column.name(), e.stage().name(), column.removed(),
                            column.dwellSecondsTotal(), column.dwellCount())));
            case StuckThresholdSet e -> new FlowBoardDocument(current.boardFlowId(), current.name(), current.archived(),
                    e.threshold().days(), current.columns(), current.workItems());
            case StuckThresholdRemoved e -> new FlowBoardDocument(current.boardFlowId(), current.name(),
                    current.archived(), null, current.columns(), current.workItems());
            // 完成紀錄另外存（FlowProjection），文件本身不變
            case WorkItemFlowResultChanged e -> current;
            case WorkItemStuckStatusChanged e -> withWorkItems(current, replaced(current.workItems(),
                    item -> item.workItemId().equals(e.workItemId().value()),
                    item -> new WorkItemEntry(item.workItemId(), item.title(), item.flowColumnId(), e.stuck(),
                            e.stuckSince())));
        };
    }

    private static FlowBoardDocument movedWorkItem(FlowBoardDocument current, WorkItemMoved e) {
        long dwellSeconds = Math.round(e.dwellTimeInFromColumn().value().toMillis() / 1000.0);
        List<ColumnEntry> columns = replacedColumn(current, e.fromFlowColumnId().value(),
                column -> new ColumnEntry(column.flowColumnId(), column.name(), column.stage(), column.removed(),
                        column.dwellSecondsTotal() + dwellSeconds, column.dwellCount() + 1));
        List<WorkItemEntry> workItems = replaced(current.workItems(),
                item -> item.workItemId().equals(e.workItemId().value()),
                item -> new WorkItemEntry(item.workItemId(), item.title(), e.toFlowColumnId().value(), item.stuck(),
                        item.stuckSince()));
        return new FlowBoardDocument(current.boardFlowId(), current.name(), current.archived(),
                current.stuckThresholdDays(), columns, workItems);
    }

    private static FlowBoardDocument withColumns(FlowBoardDocument current, List<ColumnEntry> columns) {
        return new FlowBoardDocument(current.boardFlowId(), current.name(), current.archived(),
                current.stuckThresholdDays(), columns, current.workItems());
    }

    private static FlowBoardDocument withWorkItems(FlowBoardDocument current, List<WorkItemEntry> workItems) {
        return new FlowBoardDocument(current.boardFlowId(), current.name(), current.archived(),
                current.stuckThresholdDays(), current.columns(), workItems);
    }

    private static List<ColumnEntry> replacedColumn(FlowBoardDocument current, String columnId,
            UnaryOperator<ColumnEntry> change) {
        return replaced(current.columns(), column -> column.flowColumnId().equals(columnId), change);
    }

    /** 沒被刪除的在前（保持原順序），刪除的排在最後。 */
    private static List<ColumnEntry> ordered(List<ColumnEntry> columns) {
        List<ColumnEntry> result = new ArrayList<>(columns.stream().filter(column -> !column.removed()).toList());
        result.addAll(columns.stream().filter(ColumnEntry::removed).toList());
        return List.copyOf(result);
    }

    /** 新欄位加在沒被刪除的欄位最右邊（刪除的仍排在最後）。 */
    private static List<ColumnEntry> appendedActive(List<ColumnEntry> columns, ColumnEntry added) {
        List<ColumnEntry> result = new ArrayList<>(columns.stream().filter(column -> !column.removed()).toList());
        result.add(added);
        result.addAll(columns.stream().filter(ColumnEntry::removed).toList());
        return List.copyOf(result);
    }

    /** 把一欄移到沒被刪除的欄位中的 toPosition。 */
    private static List<ColumnEntry> moved(List<ColumnEntry> columns, String columnId, int toPosition) {
        List<ColumnEntry> active = new ArrayList<>(columns.stream().filter(column -> !column.removed()).toList());
        ColumnEntry moving = active.stream().filter(column -> column.flowColumnId().equals(columnId)).findFirst()
                .orElseThrow(() -> new IllegalStateException("讀取模型裡沒有欄位 " + columnId));
        active.remove(moving);
        active.add(Math.min(toPosition, active.size()), moving);
        active.addAll(columns.stream().filter(ColumnEntry::removed).toList());
        return List.copyOf(active);
    }

    private static <T> List<T> appended(List<T> items, T item) {
        List<T> result = new ArrayList<>(items);
        result.add(item);
        return List.copyOf(result);
    }

    private static <T> List<T> replaced(List<T> items, Predicate<T> which, UnaryOperator<T> change) {
        return items.stream().map(item -> which.test(item) ? change.apply(item) : item).toList();
    }
}
