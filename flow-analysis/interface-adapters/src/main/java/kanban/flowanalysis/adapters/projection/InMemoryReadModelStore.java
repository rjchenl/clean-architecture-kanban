package kanban.flowanalysis.adapters.projection;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.ColumnEntry;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.WorkItemEntry;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ColumnMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.Delivery;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.StuckWorkItem;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.WeeklyCompletion;

/** 記憶體版流程分析讀取模型。統計方式 S1 由 {@link WeekCalendar} 決定，S2 在 {@link #flowMetrics} 算。 */
public final class InMemoryReadModelStore implements ReadModelStore {

    private static final int RECENT_WEEKS = 8;

    private record Completion(String boardFlowId, Instant weekStartsAt, long leadTimeSeconds) {
    }

    private final WeekCalendar calendar;
    private Map<String, FlowBoardDocument> boards = new LinkedHashMap<>();
    private Map<String, Completion> completions = new HashMap<>();
    private long checkpoint;
    private boolean inTransaction;

    public InMemoryReadModelStore(WeekCalendar calendar) {
        this.calendar = calendar;
    }

    @Override
    public synchronized long checkpoint() {
        return checkpoint;
    }

    @Override
    public synchronized void inExclusiveTransaction(Runnable work) {
        if (inTransaction) {
            work.run();
            return;
        }
        Map<String, FlowBoardDocument> boardsBefore = new LinkedHashMap<>(boards);
        Map<String, Completion> completionsBefore = new HashMap<>(completions);
        long checkpointBefore = checkpoint;
        inTransaction = true;
        try {
            work.run();
        } catch (RuntimeException | Error failure) {
            boards = boardsBefore;
            completions = completionsBefore;
            checkpoint = checkpointBefore;
            throw failure;
        } finally {
            inTransaction = false;
        }
    }

    @Override
    public synchronized Optional<FlowBoardDocument> boardFlowDocument(String boardFlowId) {
        return Optional.ofNullable(boards.get(boardFlowId));
    }

    @Override
    public synchronized void saveBoardFlow(FlowBoardDocument document) {
        requireTransaction();
        boards.put(document.boardFlowId(), document);
    }

    @Override
    public synchronized void saveCompletion(String boardFlowId, String workItemId, Instant completedAt,
            long leadTimeSeconds) {
        requireTransaction();
        completions.put(keyOf(boardFlowId, workItemId),
                new Completion(boardFlowId, calendar.weekStartOf(completedAt), leadTimeSeconds));
    }

    @Override
    public synchronized void removeCompletion(String boardFlowId, String workItemId) {
        requireTransaction();
        completions.remove(keyOf(boardFlowId, workItemId));
    }

    @Override
    public synchronized void advanceCheckpoint(long position) {
        requireTransaction();
        checkpoint = position;
    }

    @Override
    public synchronized void reset() {
        requireTransaction();
        boards.clear();
        completions.clear();
        checkpoint = 0;
    }

    @Override
    public synchronized List<String> allBoardFlowIds() {
        return List.copyOf(boards.keySet());
    }

    /** 統計方式 S2：各欄張數與停留平均、最近 8 週每週完成數、8 週內完成的交付時間平均與最長。平均四捨五入到整數秒。 */
    @Override
    public synchronized Optional<FlowMetrics> flowMetrics(String boardFlowId, Instant asOf) {
        FlowBoardDocument doc = boards.get(boardFlowId);
        if (doc == null) {
            return Optional.empty();
        }
        List<ColumnMetrics> columns = doc.columns().stream()
                .filter(column -> !column.removed())
                .map(column -> new ColumnMetrics(column.flowColumnId(), column.name(), column.stage(),
                        (int) doc.workItems().stream().filter(item -> item.flowColumnId().equals(column.flowColumnId())).count(),
                        column.dwellCount() == 0 ? null : Math.round((double) column.dwellSecondsTotal() / column.dwellCount())))
                .toList();
        List<Instant> weekStarts = calendar.recentWeekStarts(asOf, RECENT_WEEKS);
        Set<Instant> window = new HashSet<>(weekStarts);
        List<Completion> recent = completions.values().stream()
                .filter(completion -> completion.boardFlowId().equals(boardFlowId))
                .filter(completion -> window.contains(completion.weekStartsAt()))
                .toList();
        List<WeeklyCompletion> weeks = weekStarts.stream()
                .map(start -> new WeeklyCompletion(start,
                        (int) recent.stream().filter(completion -> completion.weekStartsAt().equals(start)).count()))
                .toList();
        Delivery delivery = recent.isEmpty()
                ? new Delivery(0, null, null)
                : new Delivery(recent.size(),
                        Math.round(recent.stream().mapToLong(Completion::leadTimeSeconds).average().orElseThrow()),
                        recent.stream().mapToLong(Completion::leadTimeSeconds).max().orElseThrow());
        List<StuckWorkItem> stuck = doc.workItems().stream()
                .filter(WorkItemEntry::stuck)
                .map(item -> new StuckWorkItem(item.workItemId(), item.title(), columnName(doc, item.flowColumnId()),
                        item.stuckSince()))
                .toList();
        return Optional.of(new FlowMetrics(doc.boardFlowId(), doc.name(), doc.archived(), doc.stuckThresholdDays(),
                columns, weeks, delivery, stuck));
    }

    private static String columnName(FlowBoardDocument doc, String columnId) {
        return doc.columns().stream().filter(column -> column.flowColumnId().equals(columnId))
                .map(ColumnEntry::name).findFirst().orElse("");
    }

    private void requireTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("讀取模型的寫入只能在 inExclusiveTransaction 之內");
        }
    }

    private static String keyOf(String boardFlowId, String workItemId) {
        return boardFlowId + "/" + workItemId;
    }
}
