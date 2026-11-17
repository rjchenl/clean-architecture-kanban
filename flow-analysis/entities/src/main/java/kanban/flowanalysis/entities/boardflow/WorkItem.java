package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;
import java.util.List;

/**
 * 工作項目：看板流程的內部物件（DDD-4，不公開）。看板上的一張卡片，在流程分析裡只看它什麼時候進出哪一欄。
 * 保留完整的停留紀錄，改階段設定時才能照新設定從頭重算（F6）。
 */
record WorkItem(WorkItemId id, WorkItemTitle title, List<ColumnVisit> visits, boolean removed,
        FlowResult result, boolean stuck) {

    WorkItem {
        visits = List.copyOf(visits);
    }

    static WorkItem added(WorkItemId id, WorkItemTitle title, FlowColumnId column, Instant at) {
        return new WorkItem(id, title, List.of(new ColumnVisit(column, at)), false, FlowResult.NONE, false);
    }

    /** 目前（或被刪除時）所在的那一欄的停留紀錄。 */
    ColumnVisit currentVisit() {
        return visits.getLast();
    }

    FlowColumnId currentColumn() {
        return currentVisit().flowColumnId();
    }

    WorkItem withTitle(WorkItemTitle newTitle) {
        return new WorkItem(id, newTitle, visits, removed, result, stuck);
    }

    WorkItem visiting(FlowColumnId column, Instant at) {
        return new WorkItem(id, title, Immutable.appended(visits, new ColumnVisit(column, at)), removed, result, stuck);
    }

    WorkItem removedNow() {
        return new WorkItem(id, title, visits, true, result, stuck);
    }

    WorkItem withResult(FlowResult newResult) {
        return new WorkItem(id, title, visits, removed, newResult, stuck);
    }

    WorkItem withStuck(boolean nowStuck) {
        return new WorkItem(id, title, visits, removed, result, nowStuck);
    }
}
