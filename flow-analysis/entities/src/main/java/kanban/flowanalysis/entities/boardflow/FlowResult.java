package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/**
 * 流程結果：一個工作項目目前的開始時間、完成時間、交付時間（看板流程的內部物件，不公開）。
 * 三個值都可以是 null：還沒開始、還沒完成。
 */
record FlowResult(Instant startedAt, Instant completedAt, LeadTime leadTime) {

    static final FlowResult NONE = new FlowResult(null, null, null);
}
