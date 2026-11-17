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

/** 階段與流程結果：F1～F6、F11～F13，以及時間倒退（Review Focus 2）。 */
class StageRulesTest {

    static final FlowColumnId TODO = new FlowColumnId("col-todo");
    static final FlowColumnId DOING = new FlowColumnId("col-doing");
    static final FlowColumnId DONE = new FlowColumnId("col-done");
    static final FlowColumnId DONE_2 = new FlowColumnId("col-done-2");
    static final FlowColumnId ARCHIVE = new FlowColumnId("col-archive");
    static final WorkItemId W1 = new WorkItemId("card-1");
    static final WorkItemId W2 = new WorkItemId("card-2");
    static final WorkItemTitle TITLE = new WorkItemTitle("串接金流");

    /** 待辦（未開始）、進行中、完成、完成二（完成）、封存區（未分類），以及待辦裡的 W1（位置 1～7）。 */
    static List<BoardFlowEvent> stagedBoard() {
        return new ArrayList<>(List.of(
                new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0),
                column(2, TODO, "待辦"), column(3, DOING, "進行中"), column(4, DONE, "完成"),
                column(5, DONE_2, "完成二"), column(6, ARCHIVE, "封存區"),
                new ColumnStageSet(FLOW, TODO, Stage.NOT_STARTED, T0),
                new ColumnStageSet(FLOW, DOING, Stage.IN_PROGRESS, T0),
                new ColumnStageSet(FLOW, DONE, Stage.DONE, T0),
                new ColumnStageSet(FLOW, DONE_2, Stage.DONE, T0),
                new WorkItemAdded(FLOW, at(7), W1, TODO, TITLE, T0)));
    }

    static FlowColumnAdded column(long position, FlowColumnId id, String name) {
        return new FlowColumnAdded(FLOW, at(position), id, new FlowColumnName(name), Stage.UNCLASSIFIED, T0);
    }

    static BoardFlowEvent[] with(List<BoardFlowEvent> base, BoardFlowEvent... more) {
        List<BoardFlowEvent> all = new ArrayList<>(base);
        all.addAll(Arrays.asList(more));
        return all.toArray(BoardFlowEvent[]::new);
    }

    static DwellTime dwell(double days) {
        return new DwellTime(Duration.ofSeconds(Math.round(days * 86_400)));
    }

    static LeadTime lead(double days) {
        return new LeadTime(Duration.ofSeconds(Math.round(days * 86_400)));
    }

    static WorkItemFlowResultChanged result(WorkItemId item, double startedDay, Double completedDay, Double leadDays,
            double atDay) {
        return new WorkItemFlowResultChanged(FLOW, item, days(startedDay),
                completedDay == null ? null : days(completedDay), leadDays == null ? null : lead(leadDays), days(atDay));
    }

    /** W1：待辦（T0）→ 進行中（第 1 天，開始）。 */
    static List<BoardFlowEvent> startedOnDay1() {
        return List.of(with(stagedBoard(),
                new WorkItemMoved(FLOW, at(8), W1, TODO, DOING, dwell(1), days(1)),
                result(W1, 1, null, null, 1)));
    }

    /** W1：…→ 完成（第 3 天，交付 2 天）。 */
    static List<BoardFlowEvent> completedOnDay3() {
        return List.of(with(startedOnDay1(),
                new WorkItemMoved(FLOW, at(9), W1, DOING, DONE, dwell(2), days(3)),
                result(W1, 1, 3.0, 2.0, 3)));
    }

    // ---------------------------------------------------------------- F1、F12、F13：指定階段

    @Test
    void 指定階段_產生ColumnStageSet() {
        given(new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0), column(2, TODO, "待辦"))
                .when(flow -> flow.setColumnStage(TODO, Stage.NOT_STARTED, days(1)))
                .thenEvents(new ColumnStageSet(FLOW, TODO, Stage.NOT_STARTED, days(1)));
    }

    @Test
    void 指定相同階段_不產生事件() {
        given(with(stagedBoard())).when(flow -> flow.setColumnStage(DOING, Stage.IN_PROGRESS, days(1))).thenNoEvents();
    }

    @Test
    void 不存在的欄位_拒絕FLOW_COLUMN_NOT_FOUND() {
        given(with(stagedBoard()))
                .when(flow -> flow.setColumnStage(new FlowColumnId("col-x"), Stage.DONE, days(1)))
                .thenRejected(RejectionReason.FLOW_COLUMN_NOT_FOUND);
    }

    @Test
    void 已刪除的欄位不能改階段_拒絕FLOW_COLUMN_REMOVED() {
        given(with(stagedBoard(), new FlowColumnRemoved(FLOW, at(8), ARCHIVE, days(1))))
                .when(flow -> flow.setColumnStage(ARCHIVE, Stage.DONE, days(2)))
                .thenRejected(RejectionReason.FLOW_COLUMN_REMOVED);
    }

    @Test
    void 看板封存後_仍可以改階段() {
        given(with(stagedBoard(), new BoardFlowArchived(FLOW, at(8), days(1))))
                .when(flow -> flow.setColumnStage(ARCHIVE, Stage.NOT_STARTED, days(2)))
                .thenEvents(new ColumnStageSet(FLOW, ARCHIVE, Stage.NOT_STARTED, days(2)));
    }

    // ---------------------------------------------------------------- F3～F5：開始、完成、交付

    @Test
    void 第一次進入進行中_記開始時間() {
        given(with(stagedBoard())).when(flow -> flow.moveWorkItem(at(8), W1, TODO, DOING, days(1)))
                .thenEvents(new WorkItemMoved(FLOW, at(8), W1, TODO, DOING, dwell(1), days(1)),
                        result(W1, 1, null, null, 1));
    }

    @Test
    void 移回未開始再回進行中_開始時間不變() {
        BoardFlowEvent[] history = with(startedOnDay1(),
                new WorkItemMoved(FLOW, at(9), W1, DOING, TODO, dwell(1), days(2)));

        given(history).when(flow -> flow.moveWorkItem(at(10), W1, TODO, DOING, days(3)))
                .thenEvents(new WorkItemMoved(FLOW, at(10), W1, TODO, DOING, dwell(1), days(3)));
    }

    @Test
    void 進入完成_記完成時間與交付時間() {
        given(with(startedOnDay1())).when(flow -> flow.moveWorkItem(at(9), W1, DOING, DONE, days(3)))
                .thenEvents(new WorkItemMoved(FLOW, at(9), W1, DOING, DONE, dwell(2), days(3)),
                        result(W1, 1, 3.0, 2.0, 3));
    }

    @Test
    void 直接從未開始進入完成_開始與完成同時_交付時間為0() {
        given(with(stagedBoard())).when(flow -> flow.moveWorkItem(at(8), W1, TODO, DONE, days(2)))
                .thenEvents(new WorkItemMoved(FLOW, at(8), W1, TODO, DONE, dwell(2), days(2)),
                        result(W1, 2, 2.0, 0.0, 2));
    }

    @Test
    void 移回進行中_不再算完成() {
        given(with(completedOnDay3())).when(flow -> flow.moveWorkItem(at(10), W1, DONE, DOING, days(4)))
                .thenEvents(new WorkItemMoved(FLOW, at(10), W1, DONE, DOING, dwell(1), days(4)),
                        result(W1, 1, null, null, 4));
    }

    @Test
    void 移回進行中後再進入完成_重新記完成時間() {
        BoardFlowEvent[] history = with(completedOnDay3(),
                new WorkItemMoved(FLOW, at(10), W1, DONE, DOING, dwell(1), days(4)),
                result(W1, 1, null, null, 4));

        given(history).when(flow -> flow.moveWorkItem(at(11), W1, DOING, DONE, days(6)))
                .thenEvents(new WorkItemMoved(FLOW, at(11), W1, DOING, DONE, dwell(2), days(6)),
                        result(W1, 1, 6.0, 5.0, 6));
    }

    @Test
    void 在兩個完成欄之間移動_完成時間不變() {
        given(with(completedOnDay3())).when(flow -> flow.moveWorkItem(at(10), W1, DONE, DONE_2, days(4)))
                .thenEvents(new WorkItemMoved(FLOW, at(10), W1, DONE, DONE_2, dwell(1), days(4)));
    }

    @Test
    void 從完成欄移到未分類欄_仍算完成() {
        // F2、F4 裁決：未分類對判斷是透明的；新增的欄位在分類前不會改變任何數字
        given(with(completedOnDay3())).when(flow -> flow.moveWorkItem(at(10), W1, DONE, ARCHIVE, days(4)))
                .thenEvents(new WorkItemMoved(FLOW, at(10), W1, DONE, ARCHIVE, dwell(1), days(4)));
    }

    @Test
    void 進出未分類欄_不影響開始時間() {
        BoardFlowEvent[] history = with(stagedBoard(),
                new WorkItemMoved(FLOW, at(8), W1, TODO, ARCHIVE, dwell(1), days(1)));

        given(history).when(flow -> flow.moveWorkItem(at(9), W1, ARCHIVE, DOING, days(2)))
                .thenEvents(new WorkItemMoved(FLOW, at(9), W1, ARCHIVE, DOING, dwell(1), days(2)),
                        result(W1, 2, null, null, 2));
    }

    @Test
    void 時間倒退時_交付時間以0計() {
        // Review Focus 2：完成的時間比開始還早
        BoardFlowEvent[] history = with(stagedBoard(),
                new WorkItemMoved(FLOW, at(8), W1, TODO, DOING, dwell(3), days(3)),
                result(W1, 3, null, null, 3));

        given(history).when(flow -> flow.moveWorkItem(at(9), W1, DOING, DONE, days(1)))
                .thenEvents(new WorkItemMoved(FLOW, at(9), W1, DOING, DONE, dwell(0), days(1)),
                        result(W1, 3, 1.0, 0.0, 1));
    }

    // ---------------------------------------------------------------- F6：改階段後從頭重算

    @Test
    void 改階段_所有工作項目從頭重算_只替結果有變的產生事件() {
        // 全部欄位都還沒分類；W1 經過待辦、進行中、完成，W2 一直在待辦
        BoardFlowEvent[] history = {
            new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0),
            column(2, TODO, "待辦"), column(3, DOING, "進行中"), column(4, DONE, "完成"),
            new WorkItemAdded(FLOW, at(5), W1, TODO, TITLE, T0),
            new WorkItemAdded(FLOW, at(6), W2, TODO, TITLE, T0),
            new WorkItemMoved(FLOW, at(7), W1, TODO, DOING, dwell(1), days(1)),
            new WorkItemMoved(FLOW, at(8), W1, DOING, DONE, dwell(2), days(3)),
        };

        given(history).when(flow -> flow.setColumnStage(DOING, Stage.IN_PROGRESS, days(5)))
                .thenEvents(new ColumnStageSet(FLOW, DOING, Stage.IN_PROGRESS, days(5)), result(W1, 1, null, null, 5));
    }

    @Test
    void 改階段_第一階段以來的歷史也照新設定算出完成() {
        BoardFlowEvent[] history = {
            new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0),
            column(2, TODO, "待辦"), column(3, DOING, "進行中"), column(4, DONE, "完成"),
            new WorkItemAdded(FLOW, at(5), W1, TODO, TITLE, T0),
            new WorkItemMoved(FLOW, at(6), W1, TODO, DOING, dwell(1), days(1)),
            new WorkItemMoved(FLOW, at(7), W1, DOING, DONE, dwell(2), days(3)),
            new ColumnStageSet(FLOW, DOING, Stage.IN_PROGRESS, days(5)),
            result(W1, 1, null, null, 5),
        };

        given(history).when(flow -> flow.setColumnStage(DONE, Stage.DONE, days(6)))
                .thenEvents(new ColumnStageSet(FLOW, DONE, Stage.DONE, days(6)), result(W1, 1, 3.0, 2.0, 6));
    }

    // ---------------------------------------------------------------- F11、F12：刪除

    @Test
    void 在完成欄被刪除_保留完成紀錄() {
        given(with(completedOnDay3())).when(flow -> flow.removeWorkItem(at(10), W1, DONE, days(4)))
                .thenEvents(new WorkItemRemoved(FLOW, at(10), W1, DONE, days(4)));
    }

    @Test
    void 在進行中欄被刪除_不算完成() {
        given(with(startedOnDay1())).when(flow -> flow.removeWorkItem(at(9), W1, DOING, days(2)))
                .thenEvents(new WorkItemRemoved(FLOW, at(9), W1, DOING, days(2)));
    }

    @Test
    void 欄位刪除後_經過它的歷史照它原本的階段判斷() {
        // W1 曾經過進行中（第 1 天開始），之後回待辦；進行中這一欄被刪掉了
        BoardFlowEvent[] history = with(startedOnDay1(),
                new WorkItemMoved(FLOW, at(9), W1, DOING, TODO, dwell(1), days(2)),
                new FlowColumnRemoved(FLOW, at(10), DOING, days(3)));

        // 待辦改成未分類，觸發重算：被刪掉的進行中仍算進行中，開始時間還是第 1 天，結果沒變
        given(history).when(flow -> flow.setColumnStage(TODO, Stage.UNCLASSIFIED, days(4)))
                .thenEvents(new ColumnStageSet(FLOW, TODO, Stage.UNCLASSIFIED, days(4)));
    }
}
