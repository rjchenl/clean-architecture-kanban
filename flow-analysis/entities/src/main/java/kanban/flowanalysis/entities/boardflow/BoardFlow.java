package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 看板流程聚合：一個看板在流程分析裡的全部紀錄（DDD-4 一致性邊界）。外部只能呼叫這裡的方法。
 *
 * <p>三種輸入：看板上發生的事實（防腐層翻譯過來，一律接受，F14）、使用者的決定（可能被拒絕）、定時檢查。
 * 什麼算開始、什麼算完成、交付時間多長、算不算卡住，全部在這裡判斷，結果寫成事件（DDD-9）。
 *
 * <p>事件溯源寫法（ES-3）：指令方法只做「檢查 → 建立事件 → record」；
 * 狀態只在 apply／on… 方法裡改變，這些方法不檢查、不會失敗、沒有副作用。
 */
public final class BoardFlow {

    private BoardFlowId id;
    private FlowBoardName name;
    private boolean archived;
    private ActivityPosition lastActivityPosition;
    private StuckThreshold stuckThreshold;
    private List<FlowColumn> columns = List.of();
    private List<WorkItem> workItems = List.of();

    private long persistedVersion;
    private final List<BoardFlowEvent> pendingEvents = new ArrayList<>();

    private BoardFlow() {
    }

    /** 看板建立了：流程分析開始記錄這個看板。 */
    public static BoardFlow create(BoardFlowId id, ActivityPosition position, FlowBoardName name, Instant at) {
        BoardFlow flow = new BoardFlow();
        flow.record(new BoardFlowCreated(id, position, name, at));
        return flow;
    }

    /** 載入聚合＝依序重播它的全部事件（ES-4）。 */
    public static BoardFlow rehydrate(List<BoardFlowEvent> history) {
        if (history.isEmpty()) {
            throw new IllegalArgumentException("history must not be empty");
        }
        BoardFlow flow = new BoardFlow();
        for (BoardFlowEvent event : history) {
            flow.apply(event);
        }
        flow.persistedVersion = history.size();
        return flow;
    }

    public BoardFlowId id() {
        return id;
    }

    /** 已存入事件紀錄的事件數，寫入時當作預期版本號（ES-6）。 */
    public long persistedVersion() {
        return persistedVersion;
    }

    /** 這次操作新產生、還沒存入的事件。 */
    public List<BoardFlowEvent> uncommittedEvents() {
        return List.copyOf(pendingEvents);
    }

    /** 事件存入成功後呼叫。 */
    public void markCommitted() {
        persistedVersion += pendingEvents.size();
        pendingEvents.clear();
    }

    // ---------------------------------------------------------------- 看板上發生的事實（F14：一律接受、重送的略過）

    /** 又收到「看板建立了」：只有重送的才會發生；更新的位置代表同一個看板建立了兩次，是程式錯誤。 */
    public void acknowledgeCreation(ActivityPosition position) {
        if (alreadyRecorded(position)) {
            return;
        }
        throw new IllegalStateException("看板流程 " + id.value() + " 已經建立過，不會再收到位置 "
                + position.value() + " 的建立動態");
    }

    public void renameBoard(ActivityPosition position, FlowBoardName newName, Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        record(new BoardFlowRenamed(id, position, newName, at));
    }

    public void archiveBoard(ActivityPosition position, Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        record(new BoardFlowArchived(id, position, at));
    }

    /** 看板加了一欄。階段一律從未分類開始（F2）。 */
    public void addColumn(ActivityPosition position, FlowColumnId columnId, FlowColumnName columnName, Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        requireColumnIdUnused(columnId);
        record(new FlowColumnAdded(id, position, columnId, columnName, Stage.UNCLASSIFIED, at));
    }

    public void renameColumn(ActivityPosition position, FlowColumnId columnId, FlowColumnName newName, Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        requireActiveColumn(columnId);
        record(new FlowColumnRenamed(id, position, columnId, newName, at));
    }

    /** 欄位換了位置。看板流程不靠欄位順序判斷，記下這件事給畫面用。 */
    public void moveColumn(ActivityPosition position, FlowColumnId columnId, FlowColumnPosition toPosition, Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        requireActiveColumn(columnId);
        record(new FlowColumnMoved(id, position, columnId, toPosition, at));
    }

    /** 欄位被刪掉了；階段保留，用來判斷曾經過那一欄的歷史（F12）。 */
    public void removeColumn(ActivityPosition position, FlowColumnId columnId, Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        requireActiveColumn(columnId);
        record(new FlowColumnRemoved(id, position, columnId, at));
    }

    public void addWorkItem(ActivityPosition position, WorkItemId workItemId, FlowColumnId columnId,
            WorkItemTitle title, Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        requireActiveColumn(columnId);
        requireWorkItemIdUnused(workItemId);
        record(new WorkItemAdded(id, position, workItemId, columnId, title, at));
        recordJudgementsOf(workItemId, at);
    }

    /** 卡片內容改了。看板管理只改了說明時標題不變，不產生事件。 */
    public void changeWorkItemTitle(ActivityPosition position, WorkItemId workItemId, WorkItemTitle title, Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        WorkItem item = requireActiveWorkItem(workItemId);
        if (item.title().equals(title)) {
            return;
        }
        record(new WorkItemTitleChanged(id, position, workItemId, title, at));
    }

    /** 卡片換欄。在來源欄的停留時間由這裡算好、寫進事件（F7）；時間倒退時以 0 計（F15 補充）。 */
    public void moveWorkItem(ActivityPosition position, WorkItemId workItemId, FlowColumnId from, FlowColumnId to,
            Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        WorkItem item = requireActiveWorkItemIn(workItemId, from);
        requireActiveColumn(to);
        DwellTime dwell = DwellTime.between(item.currentVisit().enteredAt(), at);
        record(new WorkItemMoved(id, position, workItemId, from, to, dwell, at));
        recordJudgementsOf(workItemId, at);
    }

    /** 卡片被刪掉了。被刪除不算離開，所以沒有停留時間（F7）。 */
    public void removeWorkItem(ActivityPosition position, WorkItemId workItemId, FlowColumnId from, Instant at) {
        if (alreadyRecorded(position)) {
            return;
        }
        requireActiveWorkItemIn(workItemId, from);
        record(new WorkItemRemoved(id, position, workItemId, from, at));
        recordJudgementsOf(workItemId, at);
    }

    // ---------------------------------------------------------------- 使用者的決定

    /**
     * 替一欄指定階段（F1），並照新設定從頭重算所有工作項目（F6）。
     * 已刪除的欄位不能再改階段（F12）；看板封存後照常可以改（F13）。
     */
    public void setColumnStage(FlowColumnId columnId, Stage stage, Instant at) {
        FlowColumn column = columnOf(columnId);
        if (column == null) {
            throw new BusinessRuleViolation(RejectionReason.FLOW_COLUMN_NOT_FOUND);
        }
        if (column.removed()) {
            throw new BusinessRuleViolation(RejectionReason.FLOW_COLUMN_REMOVED);
        }
        if (column.stage() == stage) {
            return;
        }
        record(new ColumnStageSet(id, columnId, stage, at));
        recordJudgements(at);
    }

    /** 設定卡住門檻（F8），並立刻重新判斷（F10）。 */
    public void setStuckThreshold(StuckThreshold threshold, Instant at) {
        if (threshold.equals(stuckThreshold)) {
            return;
        }
        record(new StuckThresholdSet(id, threshold, at));
        recordJudgements(at);
    }

    /** 取消卡住門檻：之後不再判斷卡住，原本卡住的全部不再卡住（F8、F10）。 */
    public void removeStuckThreshold(Instant at) {
        if (stuckThreshold == null) {
            return;
        }
        record(new StuckThresholdRemoved(id, at));
        recordJudgements(at);
    }

    // ---------------------------------------------------------------- 定時檢查

    /** 定時檢查卡住的工作項目（F10）。「現在」由外部帶進來，聚合不讀時鐘（ES-4）。 */
    public void checkStuckWorkItems(Instant now) {
        recordJudgements(now);
    }

    // ---------------------------------------------------------------- 判斷結果（DDD-9：只在這裡判斷，結果寫成事件）

    /**
     * 使用者的決定（改階段、改門檻）與定時檢查：照目前的設定重新判斷每一個工作項目，結果有變的才記錄——
     * 先是流程結果，再是卡住狀態。判斷時間是這次決定或檢查的時間（流程分析的現在）。
     */
    private void recordJudgements(Instant at) {
        recordFlowResults(at);
        for (WorkItem item : workItems) {
            recordStuckStatus(item, at);
        }
    }

    /**
     * 看板上的事實（工作項目新增、換欄、被刪除）：卡住只重新判斷被動到的那個工作項目，用事實自己的時間（F10、F15）。
     * 其他工作項目的卡住與這件事無關；用事實的時間重判它們，在同步積壓、看板時鐘回撥或兩邊時鐘不一致時，
     * 會把已經卡住、完全沒動的工作項目誤記成「不再卡住」（2026-11-17 獨立審查 I-5）。
     * 流程結果只看工作項目經過的欄位、與判斷時間無關，照樣全部重算。
     */
    private void recordJudgementsOf(WorkItemId touched, Instant at) {
        recordFlowResults(at);
        recordStuckStatus(workItemOf(touched), at);
    }

    private void recordFlowResults(Instant at) {
        for (WorkItem item : workItems) {
            FlowResult result = FlowRules.resultOf(item, this::stageOf);
            if (!result.equals(item.result())) {
                record(new WorkItemFlowResultChanged(id, item.id(), result.startedAt(), result.completedAt(),
                        result.leadTime(), at));
            }
        }
    }

    private void recordStuckStatus(WorkItem item, Instant at) {
        boolean stuck = FlowRules.isStuck(item, this::stageOf, stuckThreshold, at);
        if (stuck != item.stuck()) {
            record(new WorkItemStuckStatusChanged(id, item.id(), stuck,
                    stuck ? item.currentColumn() : null, stuck ? item.currentVisit().enteredAt() : null, at));
        }
    }

    /** 欄位刪除後仍保留它的階段（F12）。 */
    private Stage stageOf(FlowColumnId columnId) {
        FlowColumn column = columnOf(columnId);
        return column == null ? Stage.UNCLASSIFIED : column.stage();
    }

    // ---------------------------------------------------------------- 檢查（只在指令方法裡用）

    /** 位置不比已處理的新，就是重送的動態（F14）。 */
    private boolean alreadyRecorded(ActivityPosition position) {
        return lastActivityPosition != null && !position.isAfter(lastActivityPosition);
    }

    private void requireColumnIdUnused(FlowColumnId columnId) {
        if (columnOf(columnId) != null) {
            throw inconsistent("欄位 " + columnId.value() + " 已經存在");
        }
    }

    private FlowColumn requireActiveColumn(FlowColumnId columnId) {
        FlowColumn column = columnOf(columnId);
        if (column == null || column.removed()) {
            throw inconsistent("不認得欄位 " + columnId.value());
        }
        return column;
    }

    private void requireWorkItemIdUnused(WorkItemId workItemId) {
        if (workItemOf(workItemId) != null) {
            throw inconsistent("工作項目 " + workItemId.value() + " 已經存在");
        }
    }

    private WorkItem requireActiveWorkItem(WorkItemId workItemId) {
        WorkItem item = workItemOf(workItemId);
        if (item == null || item.removed()) {
            throw inconsistent("不認得工作項目 " + workItemId.value());
        }
        return item;
    }

    private WorkItem requireActiveWorkItemIn(WorkItemId workItemId, FlowColumnId from) {
        WorkItem item = requireActiveWorkItem(workItemId);
        if (!item.currentColumn().equals(from)) {
            throw inconsistent("工作項目 " + workItemId.value() + " 記錄在 " + item.currentColumn().value()
                    + "，看板動態說它在 " + from.value());
        }
        return item;
    }

    /** 看板上的事實和看板流程記錄的對不上：只有程式錯誤才會發生，NEVER 默默略過（設計第 7.5 節）。 */
    private IllegalStateException inconsistent(String detail) {
        return new IllegalStateException("看板流程 " + id.value() + " 的紀錄和看板動態對不上：" + detail);
    }

    private FlowColumn columnOf(FlowColumnId columnId) {
        return columns.stream().filter(column -> column.id().equals(columnId)).findFirst().orElse(null);
    }

    private WorkItem workItemOf(WorkItemId workItemId) {
        return workItems.stream().filter(item -> item.id().equals(workItemId)).findFirst().orElse(null);
    }

    // ---------------------------------------------------------------- 事件溯源（ES-3）

    private void record(BoardFlowEvent event) {
        apply(event);
        pendingEvents.add(event);
    }

    private void apply(BoardFlowEvent event) {
        switch (event) {
            case BoardFlowCreated e -> onBoardFlowCreated(e);
            case BoardFlowRenamed e -> onBoardFlowRenamed(e);
            case BoardFlowArchived e -> onBoardFlowArchived(e);
            case FlowColumnAdded e -> onFlowColumnAdded(e);
            case FlowColumnRenamed e -> onFlowColumnRenamed(e);
            case FlowColumnMoved e -> onFlowColumnMoved(e);
            case FlowColumnRemoved e -> onFlowColumnRemoved(e);
            case WorkItemAdded e -> onWorkItemAdded(e);
            case WorkItemTitleChanged e -> onWorkItemTitleChanged(e);
            case WorkItemMoved e -> onWorkItemMoved(e);
            case WorkItemRemoved e -> onWorkItemRemoved(e);
            case ColumnStageSet e -> onColumnStageSet(e);
            case StuckThresholdSet e -> onStuckThresholdSet(e);
            case StuckThresholdRemoved e -> onStuckThresholdRemoved(e);
            case WorkItemFlowResultChanged e -> onWorkItemFlowResultChanged(e);
            case WorkItemStuckStatusChanged e -> onWorkItemStuckStatusChanged(e);
        }
    }

    private void onBoardFlowCreated(BoardFlowCreated e) {
        id = e.boardFlowId();
        name = e.name();
        lastActivityPosition = e.activityPosition();
    }

    private void onBoardFlowRenamed(BoardFlowRenamed e) {
        name = e.name();
        lastActivityPosition = e.activityPosition();
    }

    private void onBoardFlowArchived(BoardFlowArchived e) {
        archived = true;
        lastActivityPosition = e.activityPosition();
    }

    private void onFlowColumnAdded(FlowColumnAdded e) {
        columns = Immutable.appended(columns, new FlowColumn(e.flowColumnId(), e.name(), e.stage(), false));
        lastActivityPosition = e.activityPosition();
    }

    private void onFlowColumnRenamed(FlowColumnRenamed e) {
        columns = Immutable.replaced(columns, column -> column.id().equals(e.flowColumnId()),
                column -> column.withName(e.name()));
        lastActivityPosition = e.activityPosition();
    }

    private void onFlowColumnMoved(FlowColumnMoved e) {
        lastActivityPosition = e.activityPosition();
    }

    private void onFlowColumnRemoved(FlowColumnRemoved e) {
        columns = Immutable.replaced(columns, column -> column.id().equals(e.flowColumnId()), FlowColumn::removedNow);
        lastActivityPosition = e.activityPosition();
    }

    private void onWorkItemAdded(WorkItemAdded e) {
        workItems = Immutable.appended(workItems,
                WorkItem.added(e.workItemId(), e.title(), e.flowColumnId(), e.occurredAt()));
        lastActivityPosition = e.activityPosition();
    }

    private void onWorkItemTitleChanged(WorkItemTitleChanged e) {
        workItems = Immutable.replaced(workItems, item -> item.id().equals(e.workItemId()),
                item -> item.withTitle(e.title()));
        lastActivityPosition = e.activityPosition();
    }

    private void onWorkItemMoved(WorkItemMoved e) {
        workItems = Immutable.replaced(workItems, item -> item.id().equals(e.workItemId()),
                item -> item.visiting(e.toFlowColumnId(), e.occurredAt()));
        lastActivityPosition = e.activityPosition();
    }

    private void onWorkItemRemoved(WorkItemRemoved e) {
        workItems = Immutable.replaced(workItems, item -> item.id().equals(e.workItemId()), WorkItem::removedNow);
        lastActivityPosition = e.activityPosition();
    }

    private void onColumnStageSet(ColumnStageSet e) {
        columns = Immutable.replaced(columns, column -> column.id().equals(e.flowColumnId()),
                column -> column.withStage(e.stage()));
    }

    private void onStuckThresholdSet(StuckThresholdSet e) {
        stuckThreshold = e.threshold();
    }

    private void onStuckThresholdRemoved(StuckThresholdRemoved e) {
        stuckThreshold = null;
    }

    private void onWorkItemFlowResultChanged(WorkItemFlowResultChanged e) {
        FlowResult result = new FlowResult(e.startedAt(), e.completedAt(), e.leadTime());
        workItems = Immutable.replaced(workItems, item -> item.id().equals(e.workItemId()),
                item -> item.withResult(result));
    }

    private void onWorkItemStuckStatusChanged(WorkItemStuckStatusChanged e) {
        workItems = Immutable.replaced(workItems, item -> item.id().equals(e.workItemId()),
                item -> item.withStuck(e.stuck()));
    }
}
