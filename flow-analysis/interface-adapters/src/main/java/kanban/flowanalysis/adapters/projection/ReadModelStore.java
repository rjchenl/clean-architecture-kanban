package kanban.flowanalysis.adapters.projection;

import java.time.Instant;
import java.util.Optional;
import kanban.flowanalysis.usecases.queries.listboardflows.BoardFlowListReader;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetricsReader;

/**
 * 流程分析的讀取模型：只由 {@link FlowProjection} 寫入（CQRS-5），可以整批清掉重建（ES-8）。
 * 統計方式 S1、S2 在這裡（設計第 7.4 節）：每週分組、最近 8 週、平均與最長，都只對已經判斷好的結果做。
 */
public interface ReadModelStore extends FlowMetricsReader, BoardFlowListReader {

    /** Projection 已經處理到事件紀錄的哪個位置；還沒處理過任何事件時為 0。 */
    long checkpoint();

    /** 同一時間只有一個在執行；work 內的寫入與 checkpoint 更新一起成功，丟例外就一起回復。 */
    void inExclusiveTransaction(Runnable work);

    /** 給 Projection 讀目前的看板流程文件。 */
    Optional<FlowBoardDocument> boardFlowDocument(String boardFlowId);

    void saveBoardFlow(FlowBoardDocument document);

    /** 工作項目完成了：記下完成時間與交付時間（秒）。同一個工作項目再存一次會覆蓋。週的起點由儲存依 S1 算好。 */
    void saveCompletion(String boardFlowId, String workItemId, Instant completedAt, long leadTimeSeconds);

    /** 工作項目不再算完成（例如移回進行中）。 */
    void removeCompletion(String boardFlowId, String workItemId);

    void advanceCheckpoint(long position);

    /** 清空全部讀取模型並把 checkpoint 設回 0。 */
    void reset();
}
