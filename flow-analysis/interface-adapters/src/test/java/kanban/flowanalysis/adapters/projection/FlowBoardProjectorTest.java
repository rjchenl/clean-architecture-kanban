package kanban.flowanalysis.adapters.projection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.ColumnEntry;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.WorkItemEntry;
import kanban.flowanalysis.entities.boardflow.ActivityPosition;
import kanban.flowanalysis.entities.boardflow.BoardFlowArchived;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.BoardFlowRenamed;
import kanban.flowanalysis.entities.boardflow.ColumnStageSet;
import kanban.flowanalysis.entities.boardflow.DwellTime;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import kanban.flowanalysis.entities.boardflow.FlowColumnAdded;
import kanban.flowanalysis.entities.boardflow.FlowColumnId;
import kanban.flowanalysis.entities.boardflow.FlowColumnMoved;
import kanban.flowanalysis.entities.boardflow.FlowColumnName;
import kanban.flowanalysis.entities.boardflow.FlowColumnPosition;
import kanban.flowanalysis.entities.boardflow.FlowColumnRemoved;
import kanban.flowanalysis.entities.boardflow.FlowColumnRenamed;
import kanban.flowanalysis.entities.boardflow.LeadTime;
import kanban.flowanalysis.entities.boardflow.Stage;
import kanban.flowanalysis.entities.boardflow.StuckThreshold;
import kanban.flowanalysis.entities.boardflow.StuckThresholdRemoved;
import kanban.flowanalysis.entities.boardflow.StuckThresholdSet;
import kanban.flowanalysis.entities.boardflow.WorkItemAdded;
import kanban.flowanalysis.entities.boardflow.WorkItemFlowResultChanged;
import kanban.flowanalysis.entities.boardflow.WorkItemId;
import kanban.flowanalysis.entities.boardflow.WorkItemMoved;
import kanban.flowanalysis.entities.boardflow.WorkItemRemoved;
import kanban.flowanalysis.entities.boardflow.WorkItemStuckStatusChanged;
import kanban.flowanalysis.entities.boardflow.WorkItemTitle;
import kanban.flowanalysis.entities.boardflow.WorkItemTitleChanged;
import org.junit.jupiter.api.Test;

/** 看板流程文件的投影：只照事件記下的值更新，不做任何「算不算」的判斷（DDD-9、設計第 0 節 4c）。 */
class FlowBoardProjectorTest {

    static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");
    static final BoardFlowId FLOW = new BoardFlowId("board-1");
    static final FlowColumnId TODO = new FlowColumnId("col-todo");
    static final FlowColumnId DOING = new FlowColumnId("col-doing");
    static final FlowColumnId DONE = new FlowColumnId("col-done");
    static final WorkItemId CARD = new WorkItemId("card-1");

    static ActivityPosition at(long position) {
        return new ActivityPosition(position);
    }

    static ColumnEntry column(String id, String name) {
        return new ColumnEntry(id, name, "UNCLASSIFIED", false, 0, 0);
    }

    /** 看板流程有待辦、進行中、完成三欄，card-1 在待辦。 */
    static FlowBoardDocument standard() {
        return new FlowBoardDocument("board-1", "產品開發", false, null,
                List.of(column("col-todo", "待辦"), column("col-doing", "進行中"), column("col-done", "完成")),
                List.of(new WorkItemEntry("card-1", "串接金流", "col-todo", false, null)));
    }

    @Test
    void 看板流程建立_新文件() {
        assertThat(FlowBoardProjector.apply(null, new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0)))
                .isEqualTo(new FlowBoardDocument("board-1", "產品開發", false, null, List.of(), List.of()));
    }

    @Test
    void 看板改名() {
        assertThat(FlowBoardProjector.apply(standard(), new BoardFlowRenamed(FLOW, at(5), new FlowBoardName("新名稱"), T0)).name())
                .isEqualTo("新名稱");
    }

    @Test
    void 看板封存() {
        assertThat(FlowBoardProjector.apply(standard(), new BoardFlowArchived(FLOW, at(5), T0)).archived()).isTrue();
    }

    @Test
    void 新增欄位_加在最右邊_階段照事件記下的() {
        FlowBoardDocument doc = FlowBoardProjector.apply(standard(),
                new FlowColumnAdded(FLOW, at(5), new FlowColumnId("col-x"), new FlowColumnName("審查"), Stage.UNCLASSIFIED, T0));

        assertThat(doc.columns()).extracting(ColumnEntry::flowColumnId)
                .containsExactly("col-todo", "col-doing", "col-done", "col-x");
        assertThat(doc.columns().getLast().stage()).isEqualTo("UNCLASSIFIED");
    }

    @Test
    void 欄位改名() {
        assertThat(FlowBoardProjector.apply(standard(), new FlowColumnRenamed(FLOW, at(5), TODO, new FlowColumnName("Todo"), T0))
                .columns().getFirst().name()).isEqualTo("Todo");
    }

    @Test
    void 調整欄位順序_位置是現有欄位中的位置() {
        FlowBoardDocument doc = FlowBoardProjector.apply(standard(),
                new FlowColumnMoved(FLOW, at(5), DONE, new FlowColumnPosition(0), T0));

        assertThat(doc.columns()).extracting(ColumnEntry::flowColumnId).containsExactly("col-done", "col-todo", "col-doing");
    }

    @Test
    void 刪除欄位_標成刪除_不再算進順序() {
        FlowBoardDocument removed = FlowBoardProjector.apply(standard(), new FlowColumnRemoved(FLOW, at(5), TODO, T0));
        FlowBoardDocument moved = FlowBoardProjector.apply(removed,
                new FlowColumnMoved(FLOW, at(6), DONE, new FlowColumnPosition(0), T0));

        assertThat(removed.columns()).filteredOn(ColumnEntry::removed).extracting(ColumnEntry::flowColumnId)
                .containsExactly("col-todo");
        assertThat(moved.columns()).filteredOn(entry -> !entry.removed()).extracting(ColumnEntry::flowColumnId)
                .containsExactly("col-done", "col-doing");
    }

    @Test
    void 新增工作項目() {
        FlowBoardDocument doc = FlowBoardProjector.apply(standard(),
                new WorkItemAdded(FLOW, at(5), new WorkItemId("card-2"), DOING, new WorkItemTitle("寫文件"), T0));

        assertThat(doc.workItems()).contains(new WorkItemEntry("card-2", "寫文件", "col-doing", false, null));
    }

    @Test
    void 工作項目改標題() {
        assertThat(FlowBoardProjector.apply(standard(), new WorkItemTitleChanged(FLOW, at(5), CARD, new WorkItemTitle("改過"), T0))
                .workItems().getFirst().title()).isEqualTo("改過");
    }

    @Test
    void 工作項目換欄_來源欄加上這次的停留時間() {
        FlowBoardDocument doc = FlowBoardProjector.apply(standard(),
                new WorkItemMoved(FLOW, at(5), CARD, TODO, DOING, new DwellTime(Duration.ofSeconds(90)), T0));

        assertThat(doc.workItems().getFirst().flowColumnId()).isEqualTo("col-doing");
        assertThat(doc.columns().getFirst()).isEqualTo(new ColumnEntry("col-todo", "待辦", "UNCLASSIFIED", false, 90, 1));
    }

    @Test
    void 工作項目刪除_不再算張數() {
        assertThat(FlowBoardProjector.apply(standard(), new WorkItemRemoved(FLOW, at(5), CARD, TODO, T0)).workItems()).isEmpty();
    }

    @Test
    void 指定欄位階段() {
        assertThat(FlowBoardProjector.apply(standard(), new ColumnStageSet(FLOW, DOING, Stage.IN_PROGRESS, T0))
                .columns().get(1).stage()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void 設定與取消卡住門檻() {
        FlowBoardDocument set = FlowBoardProjector.apply(standard(), new StuckThresholdSet(FLOW, new StuckThreshold(5), T0));

        assertThat(set.stuckThresholdDays()).isEqualTo(5);
        assertThat(FlowBoardProjector.apply(set, new StuckThresholdRemoved(FLOW, T0)).stuckThresholdDays()).isNull();
    }

    @Test
    void 流程結果更新_不改看板流程文件() {
        // 完成紀錄另外存（FlowProjection），文件本身不變
        assertThat(FlowBoardProjector.apply(standard(),
                new WorkItemFlowResultChanged(FLOW, CARD, T0, T0, new LeadTime(Duration.ZERO), T0))).isEqualTo(standard());
    }

    @Test
    void 卡住狀態改變_照事件記下() {
        Instant since = Instant.parse("2026-11-10T01:00:00Z");
        FlowBoardDocument stuck = FlowBoardProjector.apply(standard(),
                new WorkItemStuckStatusChanged(FLOW, CARD, true, TODO, since, T0));

        assertThat(stuck.workItems().getFirst()).isEqualTo(new WorkItemEntry("card-1", "串接金流", "col-todo", true, since));
        assertThat(FlowBoardProjector.apply(stuck, new WorkItemStuckStatusChanged(FLOW, CARD, false, null, null, T0))
                .workItems().getFirst()).isEqualTo(new WorkItemEntry("card-1", "串接金流", "col-todo", false, null));
    }

    @Test
    void 已刪除的工作項目再收到不再卡住_文件不變() {
        FlowBoardDocument removed = FlowBoardProjector.apply(standard(), new WorkItemRemoved(FLOW, at(5), CARD, TODO, T0));

        assertThat(FlowBoardProjector.apply(removed, new WorkItemStuckStatusChanged(FLOW, CARD, false, null, null, T0)))
                .isEqualTo(removed);
    }
}
