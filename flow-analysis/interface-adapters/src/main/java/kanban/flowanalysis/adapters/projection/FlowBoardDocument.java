package kanban.flowanalysis.adapters.projection;

import java.time.Instant;
import java.util.List;

/**
 * 一個看板流程在讀取模型裡的樣子（給 Projection 讀寫、給讀取模型算數字用）。
 * 裡面只有事件記下的值與加總、次數；「算不算」的判斷都在聚合，這裡不做（DDD-9）。
 *
 * @param columns   流程欄位；沒被刪除的依清單順序就是畫面順序，刪除的排在最後
 * @param workItems 還沒被刪除的工作項目，依第一次出現的順序
 */
public record FlowBoardDocument(String boardFlowId, String name, boolean archived, Integer stuckThresholdDays,
        List<ColumnEntry> columns, List<WorkItemEntry> workItems) {

    public FlowBoardDocument {
        columns = List.copyOf(columns);
        workItems = List.copyOf(workItems);
    }

    /**
     * @param dwellSecondsTotal 已經離開這一欄的停留時間加總（秒）
     * @param dwellCount        離開這一欄的次數
     */
    public record ColumnEntry(String flowColumnId, String name, String stage, boolean removed,
            long dwellSecondsTotal, long dwellCount) {
    }

    /** @param stuckSince 不卡住時為 null */
    public record WorkItemEntry(String workItemId, String title, String flowColumnId, boolean stuck,
            Instant stuckSince) {
    }
}
