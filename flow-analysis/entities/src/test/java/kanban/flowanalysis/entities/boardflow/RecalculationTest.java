package kanban.flowanalysis.entities.boardflow;

import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.FLOW;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.T0;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.at;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.days;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** F6（設計第 9 節第 4 項）：「先設定 A、再改成 B」的結果，必須等於「一開始就設定 B」的結果。 */
class RecalculationTest {

    static final FlowColumnId TODO = new FlowColumnId("col-todo");
    static final FlowColumnId DOING = new FlowColumnId("col-doing");
    static final FlowColumnId REVIEW = new FlowColumnId("col-review");
    static final FlowColumnId DONE = new FlowColumnId("col-done");
    static final List<WorkItemId> ITEMS = List.of(new WorkItemId("card-1"), new WorkItemId("card-2"),
            new WorkItemId("card-3"));

    static DwellTime dwell(int days) {
        return new DwellTime(Duration.ofDays(days));
    }

    /** 同一串看板上的事實：三個工作項目，會進出每一種欄位，其中一個被刪除。全部欄位一開始都沒分類。 */
    static BoardFlowEvent[] facts() {
        WorkItemId w1 = ITEMS.get(0);
        WorkItemId w2 = ITEMS.get(1);
        WorkItemId w3 = ITEMS.get(2);
        WorkItemTitle title = new WorkItemTitle("工作");
        return new BoardFlowEvent[] {
            new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0),
            new FlowColumnAdded(FLOW, at(2), TODO, new FlowColumnName("待辦"), Stage.UNCLASSIFIED, T0),
            new FlowColumnAdded(FLOW, at(3), DOING, new FlowColumnName("進行中"), Stage.UNCLASSIFIED, T0),
            new FlowColumnAdded(FLOW, at(4), REVIEW, new FlowColumnName("審查"), Stage.UNCLASSIFIED, T0),
            new FlowColumnAdded(FLOW, at(5), DONE, new FlowColumnName("完成"), Stage.UNCLASSIFIED, T0),
            new WorkItemAdded(FLOW, at(6), w1, TODO, title, T0),
            new WorkItemAdded(FLOW, at(7), w2, TODO, title, T0),
            new WorkItemAdded(FLOW, at(8), w3, DOING, title, T0),
            new WorkItemMoved(FLOW, at(9), w1, TODO, DOING, dwell(1), days(1)),
            new WorkItemMoved(FLOW, at(10), w1, DOING, REVIEW, dwell(2), days(3)),
            new WorkItemMoved(FLOW, at(11), w1, REVIEW, DONE, dwell(1), days(4)),
            new WorkItemMoved(FLOW, at(12), w2, TODO, REVIEW, dwell(2), days(2)),
            new WorkItemMoved(FLOW, at(13), w2, REVIEW, DOING, dwell(3), days(5)),
            new WorkItemMoved(FLOW, at(14), w3, DOING, DONE, dwell(6), days(6)),
            new WorkItemRemoved(FLOW, at(15), w3, DONE, days(7)),
        };
    }

    static Consumer<BoardFlow> settingB() {
        return flow -> {
            flow.setColumnStage(TODO, Stage.NOT_STARTED, days(10));
            flow.setColumnStage(DOING, Stage.IN_PROGRESS, days(10));
            flow.setColumnStage(REVIEW, Stage.IN_PROGRESS, days(10));
            flow.setColumnStage(DONE, Stage.DONE, days(10));
        };
    }

    static Consumer<BoardFlow> settingAThenB() {
        return flow -> {
            // 設定 A：審查算完成、完成欄沒分類
            flow.setColumnStage(TODO, Stage.NOT_STARTED, days(8));
            flow.setColumnStage(DOING, Stage.IN_PROGRESS, days(8));
            flow.setColumnStage(REVIEW, Stage.DONE, days(8));
            // 改成設定 B
            flow.setColumnStage(REVIEW, Stage.IN_PROGRESS, days(10));
            flow.setColumnStage(DONE, Stage.DONE, days(10));
        };
    }

    /** 每個工作項目最後的判斷結果（開始、完成、交付）；沒有任何結果事件的算「還沒開始」。 */
    static Map<WorkItemId, List<Object>> finalResults(List<BoardFlowEvent> produced) {
        Map<WorkItemId, List<Object>> results = new HashMap<>();
        ITEMS.forEach(item -> results.put(item, java.util.Arrays.asList(null, null, null)));
        for (BoardFlowEvent event : produced) {
            if (event instanceof WorkItemFlowResultChanged changed) {
                results.put(changed.workItemId(),
                        java.util.Arrays.asList(changed.startedAt(), changed.completedAt(), changed.leadTime()));
            }
        }
        return results;
    }

    @Test
    void 先設定A再改成B_結果等於一開始就設定B() {
        Map<WorkItemId, List<Object>> viaA = finalResults(given(facts()).when(settingAThenB()).events());
        Map<WorkItemId, List<Object>> directlyB = finalResults(given(facts()).when(settingB()).events());

        assertThat(viaA).isEqualTo(directlyB);
        // 確認這個比較有意義：設定 B 底下三個工作項目都有結果
        assertThat(directlyB.get(ITEMS.get(0))).containsExactly(days(1), days(4), new LeadTime(Duration.ofDays(3)));
        assertThat(directlyB.get(ITEMS.get(1))).containsExactly(days(2), null, null);
        assertThat(directlyB.get(ITEMS.get(2))).containsExactly(T0, days(6), new LeadTime(Duration.ofDays(6)));
    }
}
