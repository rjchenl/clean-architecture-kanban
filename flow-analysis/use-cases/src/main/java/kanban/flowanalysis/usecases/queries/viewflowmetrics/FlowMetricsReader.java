package kanban.flowanalysis.usecases.queries.viewflowmetrics;

import java.time.Instant;
import java.util.Optional;

/** 讀流程數字（讀取模型）。「最近 8 週」怎麼算由讀取模型負責（統計方式 S1、S2）。 */
public interface FlowMetricsReader {

    Optional<FlowMetrics> flowMetrics(String boardFlowId, Instant asOf);
}
