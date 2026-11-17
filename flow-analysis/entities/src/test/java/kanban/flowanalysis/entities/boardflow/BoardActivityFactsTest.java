package kanban.flowanalysis.entities.boardflow;

import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.FLOW;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.T0;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.at;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.days;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.given;
import static kanban.flowanalysis.entities.boardflow.BoardFlowScenario.whenCreating;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** 看板流程記錄看板上發生的事實：一律接受（F14），時間用看板上的時間（F15），停留時間（F7）。 */
class BoardActivityFactsTest {

    static final FlowColumnId TODO = new FlowColumnId("col-todo");
    static final FlowColumnId DOING = new FlowColumnId("col-doing");
    static final FlowColumnId UNKNOWN_COLUMN = new FlowColumnId("col-unknown");
    static final WorkItemId W1 = new WorkItemId("card-1");
    static final WorkItemId UNKNOWN_ITEM = new WorkItemId("card-unknown");
    static final FlowBoardName NAME = new FlowBoardName("產品開發");
    static final WorkItemTitle TITLE = new WorkItemTitle("串接金流");

    /** 共用的歷史：看板流程（位置 1）、待辦（2）、進行中（3）、待辦裡一個工作項目（4）。 */
    static BoardFlowEvent[] standardHistory() {
        return new BoardFlowEvent[] {
            new BoardFlowCreated(FLOW, at(1), NAME, T0),
            new FlowColumnAdded(FLOW, at(2), TODO, new FlowColumnName("待辦"), Stage.UNCLASSIFIED, T0),
            new FlowColumnAdded(FLOW, at(3), DOING, new FlowColumnName("進行中"), Stage.UNCLASSIFIED, T0),
            new WorkItemAdded(FLOW, at(4), W1, TODO, TITLE, T0),
        };
    }

    @Test
    void 建立看板流程_產生BoardFlowCreated() {
        whenCreating(() -> BoardFlow.create(FLOW, at(1), NAME, T0))
                .thenEvents(new BoardFlowCreated(FLOW, at(1), NAME, T0));
    }

    static Stream<Arguments> factsAtAnOldPosition() {
        return Stream.of(
                Arguments.of("建立看板", (Consumer<BoardFlow>) flow -> flow.acknowledgeCreation(at(1))),
                Arguments.of("看板改名", (Consumer<BoardFlow>) flow -> flow.renameBoard(at(4), new FlowBoardName("新"), days(1))),
                Arguments.of("看板封存", (Consumer<BoardFlow>) flow -> flow.archiveBoard(at(4), days(1))),
                Arguments.of("新增欄位", (Consumer<BoardFlow>) flow -> flow.addColumn(at(2), TODO, new FlowColumnName("待辦"), T0)),
                Arguments.of("欄位改名", (Consumer<BoardFlow>) flow -> flow.renameColumn(at(4), TODO, new FlowColumnName("新"), days(1))),
                Arguments.of("調整欄位順序", (Consumer<BoardFlow>) flow -> flow.moveColumn(at(4), TODO, new FlowColumnPosition(1), days(1))),
                Arguments.of("刪除欄位", (Consumer<BoardFlow>) flow -> flow.removeColumn(at(4), DOING, days(1))),
                Arguments.of("新增工作項目", (Consumer<BoardFlow>) flow -> flow.addWorkItem(at(4), W1, TODO, TITLE, T0)),
                Arguments.of("改標題", (Consumer<BoardFlow>) flow -> flow.changeWorkItemTitle(at(4), W1, new WorkItemTitle("新"), days(1))),
                Arguments.of("換欄", (Consumer<BoardFlow>) flow -> flow.moveWorkItem(at(4), W1, TODO, DOING, days(1))),
                Arguments.of("刪除工作項目", (Consumer<BoardFlow>) flow -> flow.removeWorkItem(at(4), W1, TODO, days(1))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factsAtAnOldPosition")
    void 同一筆動態收到兩次_只記一次(String label, Consumer<BoardFlow> fact) {
        // 位置不比已處理的新，就是重送的動態（防腐層在兩步之間當掉時會重送），直接略過（F14）
        given(standardHistory()).when(fact).thenNoEvents();
    }

    @Test
    void 位置比已處理的舊_略過() {
        given(standardHistory()).when(flow -> flow.renameBoard(at(2), new FlowBoardName("新名稱"), days(1)))
                .thenNoEvents();
    }

    @Test
    void 已經建立過又收到更新位置的建立_丟IllegalStateException() {
        given(standardHistory()).when(flow -> flow.acknowledgeCreation(at(9))).thenFails(IllegalStateException.class);
    }

    @Test
    void 新增欄位_階段是未分類() {
        given(new BoardFlowCreated(FLOW, at(1), NAME, T0))
                .when(flow -> flow.addColumn(at(2), TODO, new FlowColumnName("待辦"), days(1)))
                .thenEvents(new FlowColumnAdded(FLOW, at(2), TODO, new FlowColumnName("待辦"), Stage.UNCLASSIFIED, days(1)));
    }

    @Test
    void 改欄名_產生FlowColumnRenamed() {
        given(standardHistory()).when(flow -> flow.renameColumn(at(5), TODO, new FlowColumnName("Todo"), days(1)))
                .thenEvents(new FlowColumnRenamed(FLOW, at(5), TODO, new FlowColumnName("Todo"), days(1)));
    }

    @Test
    void 調整欄位順序_產生FlowColumnMoved() {
        given(standardHistory()).when(flow -> flow.moveColumn(at(5), DOING, new FlowColumnPosition(0), days(1)))
                .thenEvents(new FlowColumnMoved(FLOW, at(5), DOING, new FlowColumnPosition(0), days(1)));
    }

    @Test
    void 刪除欄位_產生FlowColumnRemoved() {
        given(standardHistory()).when(flow -> flow.removeColumn(at(5), DOING, days(1)))
                .thenEvents(new FlowColumnRemoved(FLOW, at(5), DOING, days(1)));
    }

    @Test
    void 新增工作項目_產生WorkItemAdded_所在欄未分類時沒有流程結果() {
        WorkItemId w2 = new WorkItemId("card-2");
        given(standardHistory()).when(flow -> flow.addWorkItem(at(5), w2, DOING, TITLE, days(1)))
                .thenEvents(new WorkItemAdded(FLOW, at(5), w2, DOING, TITLE, days(1)));
    }

    @Test
    void 改標題_產生WorkItemTitleChanged() {
        given(standardHistory()).when(flow -> flow.changeWorkItemTitle(at(5), W1, new WorkItemTitle("改過"), days(1)))
                .thenEvents(new WorkItemTitleChanged(FLOW, at(5), W1, new WorkItemTitle("改過"), days(1)));
    }

    @Test
    void 標題沒變_不產生事件() {
        // 看板管理只改了卡片說明時，看板動態照樣送來，標題沒變
        given(standardHistory()).when(flow -> flow.changeWorkItemTitle(at(5), W1, TITLE, days(1))).thenNoEvents();
    }

    @Test
    void 換欄_產生WorkItemMoved並帶在來源欄的停留時間() {
        given(standardHistory()).when(flow -> flow.moveWorkItem(at(5), W1, TODO, DOING, days(2)))
                .thenEvents(new WorkItemMoved(FLOW, at(5), W1, TODO, DOING, new DwellTime(Duration.ofDays(2)), days(2)));
    }

    @Test
    void 看板上的時間倒退_停留時間以0計() {
        // F15 補充、Review Focus 2：看板上的時間比進入那一欄還早，不丟例外
        given(standardHistory()).when(flow -> flow.moveWorkItem(at(5), W1, TODO, DOING, days(-1)))
                .thenEvents(new WorkItemMoved(FLOW, at(5), W1, TODO, DOING, new DwellTime(Duration.ZERO), days(-1)));
    }

    @Test
    void 刪除工作項目_產生WorkItemRemoved_沒有停留時間() {
        // F7：被刪除不算離開所在欄
        given(standardHistory()).when(flow -> flow.removeWorkItem(at(5), W1, TODO, days(3)))
                .thenEvents(new WorkItemRemoved(FLOW, at(5), W1, TODO, days(3)));
    }

    @Test
    void 事件時間用看板上發生的時間() {
        // F15：不是流程分析收到的時間
        given(standardHistory()).when(flow -> flow.renameBoard(at(5), new FlowBoardName("新名稱"), days(3)))
                .thenEvents(new BoardFlowRenamed(FLOW, at(5), new FlowBoardName("新名稱"), days(3)));
    }

    @Test
    void 看板封存_照常記錄() {
        given(standardHistory()).when(flow -> flow.archiveBoard(at(5), days(1)))
                .thenEvents(new BoardFlowArchived(FLOW, at(5), days(1)));
    }

    static Stream<Arguments> factsAboutUnknownThings() {
        return Stream.of(
                Arguments.of("欄位改名", (Consumer<BoardFlow>) flow -> flow.renameColumn(at(5), UNKNOWN_COLUMN, new FlowColumnName("新"), days(1))),
                Arguments.of("調整欄位順序", (Consumer<BoardFlow>) flow -> flow.moveColumn(at(5), UNKNOWN_COLUMN, new FlowColumnPosition(0), days(1))),
                Arguments.of("刪除欄位", (Consumer<BoardFlow>) flow -> flow.removeColumn(at(5), UNKNOWN_COLUMN, days(1))),
                Arguments.of("新增到不存在的欄", (Consumer<BoardFlow>) flow -> flow.addWorkItem(at(5), new WorkItemId("card-2"), UNKNOWN_COLUMN, TITLE, days(1))),
                Arguments.of("重複新增同一個工作項目", (Consumer<BoardFlow>) flow -> flow.addWorkItem(at(5), W1, TODO, TITLE, days(1))),
                Arguments.of("重複新增同一個欄位", (Consumer<BoardFlow>) flow -> flow.addColumn(at(5), TODO, new FlowColumnName("待辦"), days(1))),
                Arguments.of("改不存在的工作項目標題", (Consumer<BoardFlow>) flow -> flow.changeWorkItemTitle(at(5), UNKNOWN_ITEM, TITLE, days(1))),
                Arguments.of("移動不存在的工作項目", (Consumer<BoardFlow>) flow -> flow.moveWorkItem(at(5), UNKNOWN_ITEM, TODO, DOING, days(1))),
                Arguments.of("移到不存在的欄", (Consumer<BoardFlow>) flow -> flow.moveWorkItem(at(5), W1, TODO, UNKNOWN_COLUMN, days(1))),
                Arguments.of("刪除不存在的工作項目", (Consumer<BoardFlow>) flow -> flow.removeWorkItem(at(5), UNKNOWN_ITEM, TODO, days(1))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factsAboutUnknownThings")
    void 引用不存在的欄位或工作項目_丟IllegalStateException且沒有新事件(String label, Consumer<BoardFlow> fact) {
        // Review Focus 1：只有程式錯誤才會發生；寧可停住讓人發現，也不默默略過、算出錯的數字
        given(standardHistory()).when(fact).thenFails(IllegalStateException.class);
    }

    @Test
    void 來源欄和記錄的所在欄不一致_丟IllegalStateException() {
        given(standardHistory()).when(flow -> flow.moveWorkItem(at(5), W1, DOING, TODO, days(1)))
                .thenFails(IllegalStateException.class);
        given(standardHistory()).when(flow -> flow.removeWorkItem(at(5), W1, DOING, days(1)))
                .thenFails(IllegalStateException.class);
    }

    @Test
    void 已刪除的工作項目不會再有動態() {
        BoardFlowEvent[] history = Stream.concat(Stream.of(standardHistory()),
                Stream.of(new WorkItemRemoved(FLOW, at(5), W1, TODO, days(1)))).toArray(BoardFlowEvent[]::new);

        given(history).when(flow -> flow.moveWorkItem(at(6), W1, TODO, DOING, days(2)))
                .thenFails(IllegalStateException.class);
    }
}
