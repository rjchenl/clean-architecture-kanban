package kanban.flowanalysis.usecases.commands.recordboardactivity;

import java.time.Instant;
import kanban.flowanalysis.usecases.commands.BoardChange;

/**
 * 一件看板上發生的事，已經翻成流程分析的用語。每一種變化用到哪些欄位見 {@link BoardChange}。
 *
 * @param activityPosition 這筆看板動態的位置，用來認出重送的動態（F14）
 * @param occurredAt       看板上實際發生的時間（F15）
 */
public record RecordBoardActivityRequest(Long activityPosition, BoardChange change, String boardFlowId, Instant occurredAt, String boardName,
        String flowColumnId, String flowColumnName, Integer flowColumnPosition, String workItemId, String workItemTitle,
        String fromFlowColumnId, String toFlowColumnId) {
}
