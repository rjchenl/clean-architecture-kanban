package kanban.flowanalysis.entities.boardflow;

import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.FLOW;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.T0;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.at;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.days;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.given;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 卡住：F8～F10，以及被刪除的不再卡住（F11）。 */
class StuckRulesTest {

    static final FlowColumnId TODO = new FlowColumnId("col-todo");
    static final FlowColumnId DOING = new FlowColumnId("col-doing");
    static final FlowColumnId DONE = new FlowColumnId("col-done");
    static final WorkItemId W1 = new WorkItemId("card-1");

    static FlowColumnAdded column(long position, FlowColumnId id, String name) {
        return new FlowColumnAdded(FLOW, at(position), id, new FlowColumnName(name), Stage.UNCLASSIFIED, T0);
    }

    /** 待辦（未開始）、進行中、完成，W1 從 T0 起在待辦。 */
    static List<BoardFlowEvent> board() {
        return List.of(
                new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0),
                column(2, TODO, "待辦"), column(3, DOING, "進行中"), column(4, DONE, "完成"),
                new ColumnStageSet(FLOW, TODO, Stage.NOT_STARTED, T0),
                new ColumnStageSet(FLOW, DOING, Stage.IN_PROGRESS, T0),
                new ColumnStageSet(FLOW, DONE, Stage.DONE, T0),
                new WorkItemAdded(FLOW, at(5), W1, TODO, new WorkItemTitle("串接金流"), T0));
    }

    /** W1 從第 1 天起在進行中。 */
    static List<BoardFlowEvent> inProgressSinceDay1() {
        return plus(board(),
                new WorkItemMoved(FLOW, at(6), W1, TODO, DOING, new DwellTime(Duration.ofDays(1)), days(1)),
                new WorkItemFlowResultChanged(FLOW, W1, days(1), null, null, days(1)));
    }

    /** 門檻 5 天。 */
    static List<BoardFlowEvent> thresholdFiveDays() {
        return plus(inProgressSinceDay1(), new StuckThresholdSet(FLOW, new StuckThreshold(5), days(1)));
    }

    /** 第 6 天的定時檢查把 W1 標成卡住。 */
    static List<BoardFlowEvent> stuckSinceDay6() {
        return plus(thresholdFiveDays(), stuck(6));
    }

    static List<BoardFlowEvent> plus(List<BoardFlowEvent> base, BoardFlowEvent... more) {
        List<BoardFlowEvent> all = new ArrayList<>(base);
        all.addAll(Arrays.asList(more));
        return List.copyOf(all);
    }

    static BoardFlowEvent[] array(List<BoardFlowEvent> events) {
        return events.toArray(BoardFlowEvent[]::new);
    }

    static WorkItemStuckStatusChanged stuck(double atDay) {
        return new WorkItemStuckStatusChanged(FLOW, W1, true, DOING, days(1), days(atDay));
    }

    static WorkItemStuckStatusChanged unstuck(double atDay) {
        return new WorkItemStuckStatusChanged(FLOW, W1, false, null, null, days(atDay));
    }

    // ---------------------------------------------------------------- 判斷時間不倒退（2026-11-17 獨立審查 I-5）

    /**
     * 同步積壓、看板時鐘回撥或兩邊時鐘不一致時，事實的看板時間會早於上一次判斷的時間。用那個較早的時間重判全部，
     * 已經卡住、完全沒動的工作項目會被記成「不再卡住」——一筆假的事實。看板上的事實只重判被動到的工作項目（F10）。
     */
    @Test
    void 已經卡住_晚到的事實帶著較早的看板時間_沒動的工作項目不會翻回不卡住() {
        WorkItemId w2 = new WorkItemId("card-2");
        given(array(stuckSinceDay6()))
                .when(flow -> flow.addWorkItem(at(7), w2, TODO, new WorkItemTitle("寫文件"), days(4)))
                .thenEvents(new WorkItemAdded(FLOW, at(7), w2, TODO, new WorkItemTitle("寫文件"), days(4)));
    }

    @Test
    void 晚到的換欄事實_被動到的工作項目用事實的時間判斷_不再卡住() {
        given(array(stuckSinceDay6()))
                .when(flow -> flow.moveWorkItem(at(7), W1, DOING, DONE, days(4)))
                .thenEvents(
                        new WorkItemMoved(FLOW, at(7), W1, DOING, DONE, new DwellTime(Duration.ofDays(3)), days(4)),
                        new WorkItemFlowResultChanged(FLOW, W1, days(1), days(4), new LeadTime(Duration.ofDays(3)), days(4)),
                        unstuck(4));
    }

    @Test
    void 晚到的事實只重判被動到的工作項目_另一張卡住的不受影響() {
        WorkItemId w2 = new WorkItemId("card-2");
        BoardFlowEvent[] history = array(plus(stuckSinceDay6(),
                new WorkItemAdded(FLOW, at(7), w2, DOING, new WorkItemTitle("寫文件"), days(6)),
                new WorkItemFlowResultChanged(FLOW, w2, days(6), null, null, days(6))));
        given(history)
                .when(flow -> flow.moveWorkItem(at(8), w2, DOING, TODO, days(5)))
                // W1 沒有任何事件；W2 移回未開始仍保留開始時間（F3），所以也沒有流程結果的變化
                .thenEvents(new WorkItemMoved(FLOW, at(8), w2, DOING, TODO, new DwellTime(Duration.ZERO), days(5)));
    }

    // ---------------------------------------------------------------- F8：門檻

    @Test
    void 設定門檻_產生StuckThresholdSet() {
        given(array(inProgressSinceDay1())).when(flow -> flow.setStuckThreshold(new StuckThreshold(5), days(1)))
                .thenEvents(new StuckThresholdSet(FLOW, new StuckThreshold(5), days(1)));
    }

    @Test
    void 設定相同門檻_不產生事件() {
        given(array(thresholdFiveDays())).when(flow -> flow.setStuckThreshold(new StuckThreshold(5), days(2)))
                .thenNoEvents();
    }

    @Test
    void 取消門檻_產生StuckThresholdRemoved() {
        given(array(thresholdFiveDays())).when(flow -> flow.removeStuckThreshold(days(2)))
                .thenEvents(new StuckThresholdRemoved(FLOW, days(2)));
    }

    @Test
    void 沒有門檻又取消_不產生事件() {
        given(array(inProgressSinceDay1())).when(flow -> flow.removeStuckThreshold(days(2))).thenNoEvents();
    }

    @Test
    void 沒有門檻_定時檢查不產生事件() {
        given(array(inProgressSinceDay1())).when(flow -> flow.checkStuckWorkItems(days(30))).thenNoEvents();
    }

    // ---------------------------------------------------------------- F9：只有進行中、待滿門檻才算卡住

    @Test
    void 在進行中欄待滿門檻_定時檢查後變成卡住() {
        given(array(thresholdFiveDays())).when(flow -> flow.checkStuckWorkItems(days(6))).thenEvents(stuck(6));
    }

    @Test
    void 還差一秒_不算卡住() {
        given(array(thresholdFiveDays()))
                .when(flow -> flow.checkStuckWorkItems(days(6).minusSeconds(1)))
                .thenNoEvents();
    }

    @Test
    void 在未開始或完成欄待再久_都不算卡住() {
        List<BoardFlowEvent> history = plus(board(),
                new StuckThresholdSet(FLOW, new StuckThreshold(5), T0),
                new WorkItemAdded(FLOW, at(6), new WorkItemId("card-2"), DONE, new WorkItemTitle("已完成"), T0),
                new WorkItemFlowResultChanged(FLOW, new WorkItemId("card-2"), T0, T0, new LeadTime(Duration.ZERO), T0));

        given(array(history)).when(flow -> flow.checkStuckWorkItems(days(100))).thenNoEvents();
    }

    @Test
    void 已經卡住_再檢查一次_不重複產生事件() {
        given(array(stuckSinceDay6())).when(flow -> flow.checkStuckWorkItems(days(7))).thenNoEvents();
    }

    // ---------------------------------------------------------------- F10、F11：重新判斷的時機

    @Test
    void 換欄_不再卡住() {
        given(array(stuckSinceDay6())).when(flow -> flow.moveWorkItem(at(7), W1, DOING, DONE, days(7)))
                .thenEvents(
                        new WorkItemMoved(FLOW, at(7), W1, DOING, DONE, new DwellTime(Duration.ofDays(6)), days(7)),
                        new WorkItemFlowResultChanged(FLOW, W1, days(1), days(7), new LeadTime(Duration.ofDays(6)), days(7)),
                        unstuck(7));
    }

    @Test
    void 被刪除_不再卡住() {
        given(array(stuckSinceDay6())).when(flow -> flow.removeWorkItem(at(7), W1, DOING, days(7)))
                .thenEvents(new WorkItemRemoved(FLOW, at(7), W1, DOING, days(7)), unstuck(7));
    }

    @Test
    void 改階段讓所在欄變成進行中_立刻判斷() {
        List<BoardFlowEvent> history = plus(board(), new StuckThresholdSet(FLOW, new StuckThreshold(5), T0));

        given(array(history)).when(flow -> flow.setColumnStage(TODO, Stage.IN_PROGRESS, days(6)))
                .thenEvents(
                        new ColumnStageSet(FLOW, TODO, Stage.IN_PROGRESS, days(6)),
                        new WorkItemFlowResultChanged(FLOW, W1, T0, null, null, days(6)),
                        new WorkItemStuckStatusChanged(FLOW, W1, true, TODO, T0, days(6)));
    }

    @Test
    void 改門檻_立刻重新判斷() {
        List<BoardFlowEvent> history = plus(inProgressSinceDay1(), new StuckThresholdSet(FLOW, new StuckThreshold(10), days(1)));

        given(array(history)).when(flow -> flow.setStuckThreshold(new StuckThreshold(3), days(5)))
                .thenEvents(new StuckThresholdSet(FLOW, new StuckThreshold(3), days(5)), stuck(5));
    }

    @Test
    void 取消門檻_全部不再卡住() {
        given(array(stuckSinceDay6())).when(flow -> flow.removeStuckThreshold(days(8)))
                .thenEvents(new StuckThresholdRemoved(FLOW, days(8)), unstuck(8));
    }
}
