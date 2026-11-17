package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 領域事件：聚合對一個工作項目有了新的判斷結果（F3～F6、F11）。三個值都可以是 null：還沒開始、還沒完成。 */
public record WorkItemFlowResultChanged(BoardFlowId boardFlowId, WorkItemId workItemId, Instant startedAt, Instant completedAt, LeadTime leadTime, Instant occurredAt) implements BoardFlowEvent {
}
