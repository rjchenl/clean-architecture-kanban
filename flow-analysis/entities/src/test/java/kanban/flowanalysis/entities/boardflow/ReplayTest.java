package kanban.flowanalysis.entities.boardflow;

import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.FLOW;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.T0;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.at;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.days;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** ES-4：載入看板流程＝重播它的事件；同一串事件重播多次，結果必須相同。 */
class ReplayTest {

    static final FlowColumnId TODO = new FlowColumnId("col-todo");
    static final FlowColumnId DOING = new FlowColumnId("col-doing");
    static final FlowColumnId DONE = new FlowColumnId("col-done");
    static final FlowColumnId OLD = new FlowColumnId("col-old");
    static final WorkItemId W1 = new WorkItemId("card-1");
    static final WorkItemId W2 = new WorkItemId("card-2");

    /** 16 種事件各至少一筆。 */
    static List<BoardFlowEvent> history() {
        return List.of(
                new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0),
                new BoardFlowRenamed(FLOW, at(2), new FlowBoardName("產品開發 2026"), T0),
                new FlowColumnAdded(FLOW, at(3), TODO, new FlowColumnName("待辦"), Stage.UNCLASSIFIED, T0),
                new FlowColumnAdded(FLOW, at(4), DOING, new FlowColumnName("進行中"), Stage.UNCLASSIFIED, T0),
                new FlowColumnAdded(FLOW, at(5), DONE, new FlowColumnName("完成"), Stage.UNCLASSIFIED, T0),
                new FlowColumnAdded(FLOW, at(6), OLD, new FlowColumnName("舊欄"), Stage.UNCLASSIFIED, T0),
                new FlowColumnRenamed(FLOW, at(7), DOING, new FlowColumnName("開發中"), T0),
                new FlowColumnMoved(FLOW, at(8), DONE, new FlowColumnPosition(0), T0),
                new FlowColumnRemoved(FLOW, at(9), OLD, T0),
                new ColumnStageSet(FLOW, TODO, Stage.NOT_STARTED, T0),
                new ColumnStageSet(FLOW, DOING, Stage.IN_PROGRESS, T0),
                new ColumnStageSet(FLOW, DONE, Stage.DONE, T0),
                new WorkItemAdded(FLOW, at(10), W1, TODO, new WorkItemTitle("串接金流"), T0),
                new WorkItemAdded(FLOW, at(11), W2, TODO, new WorkItemTitle("寫文件"), T0),
                new WorkItemTitleChanged(FLOW, at(12), W2, new WorkItemTitle("寫使用說明"), T0),
                new StuckThresholdSet(FLOW, new StuckThreshold(2), T0),
                new WorkItemMoved(FLOW, at(13), W1, TODO, DOING, new DwellTime(Duration.ofDays(1)), days(1)),
                new WorkItemFlowResultChanged(FLOW, W1, days(1), null, null, days(1)),
                new WorkItemStuckStatusChanged(FLOW, W1, true, DOING, days(1), days(3)),
                new WorkItemRemoved(FLOW, at(14), W2, TODO, days(4)),
                new StuckThresholdRemoved(FLOW, days(5)),
                new WorkItemStuckStatusChanged(FLOW, W1, false, null, null, days(5)),
                new BoardFlowArchived(FLOW, at(15), days(6)));
    }

    @Test
    void 歷史涵蓋全部16種事件() {
        assertThat(history().stream().map(event -> event.getClass().getSimpleName()).collect(Collectors.toSet()))
                .hasSize(BoardFlowEvent.class.getPermittedSubclasses().length);
    }

    @Test
    void 同一串事件重播兩次_後續指令的結果相同() {
        BoardFlow first = BoardFlow.rehydrate(history());
        BoardFlow second = BoardFlow.rehydrate(history());

        // 會被拒絕的指令：已刪除的欄位不能改階段
        assertThatThrownBy(() -> first.setColumnStage(OLD, Stage.DONE, days(7)))
                .isInstanceOfSatisfying(BusinessRuleViolation.class,
                        violation -> assertThat(violation.reason()).isEqualTo(RejectionReason.FLOW_COLUMN_REMOVED));
        assertThatThrownBy(() -> second.setColumnStage(OLD, Stage.DONE, days(7)))
                .isInstanceOfSatisfying(BusinessRuleViolation.class,
                        violation -> assertThat(violation.reason()).isEqualTo(RejectionReason.FLOW_COLUMN_REMOVED));

        // 會成功的指令：重新設定門檻、把進行中改成完成，兩邊產生的事件完全相同
        first.setStuckThreshold(new StuckThreshold(1), days(7));
        second.setStuckThreshold(new StuckThreshold(1), days(7));
        first.setColumnStage(DOING, Stage.DONE, days(8));
        second.setColumnStage(DOING, Stage.DONE, days(8));

        assertThat(first.uncommittedEvents()).isNotEmpty().isEqualTo(second.uncommittedEvents());
        assertThat(first.persistedVersion()).isEqualTo(second.persistedVersion()).isEqualTo(history().size());
    }

    @Test
    void 重播不產生未提交事件() {
        assertThat(BoardFlow.rehydrate(history()).uncommittedEvents()).isEmpty();
    }

    @Test
    void 重播後的重送判斷照樣成立() {
        BoardFlow flow = BoardFlow.rehydrate(history());

        flow.renameBoard(at(15), new FlowBoardName("重送"), days(9));

        assertThat(flow.uncommittedEvents()).as("位置 15 已經處理過").isEmpty();
    }
}
