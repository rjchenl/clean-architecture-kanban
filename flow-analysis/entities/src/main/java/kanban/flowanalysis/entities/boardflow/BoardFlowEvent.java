package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/**
 * 看板流程裡已經發生的一件事（DDD-7：過去式、不可變）。
 *
 * <p>三類：看板上發生的事實（防腐層翻譯過來，帶看板動態位置，時間是看板上實際發生的時間，F15）、
 * 使用者的決定、聚合的判斷結果（查詢端與 Projection 都不准判斷，所以判斷結果也寫成事件）。
 */
public sealed interface BoardFlowEvent permits
        BoardFlowCreated, BoardFlowRenamed, BoardFlowArchived,
        FlowColumnAdded, FlowColumnRenamed, FlowColumnMoved, FlowColumnRemoved,
        WorkItemAdded, WorkItemTitleChanged, WorkItemMoved, WorkItemRemoved,
        ColumnStageSet, StuckThresholdSet, StuckThresholdRemoved,
        WorkItemFlowResultChanged, WorkItemStuckStatusChanged {

    BoardFlowId boardFlowId();

    /** 發生時間，由外部傳入聚合，聚合自己不讀時鐘（ES-4）。 */
    Instant occurredAt();
}
