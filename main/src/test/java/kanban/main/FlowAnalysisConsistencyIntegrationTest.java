package kanban.main;

import static kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind.BOARD_CREATED;
import static kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind.CARD_ADDED;
import static kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind.COLUMN_ADDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import kanban.boardmanagement.adapters.projection.BoardActivityLogStore;
import kanban.boardmanagement.adapters.projection.BoardActivityProjection;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.flowanalysis.adapters.presenters.CommandOutcome;
import kanban.flowanalysis.usecases.commands.gateway.ConcurrentUpdateException;
import kanban.flowanalysis.usecases.commands.gateway.EventStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** CQRS-6：第二階段設計第 7.6 節寫的六種情況（含 4b），各有一支測試證明真的如描述般運作；另加看板動態紀錄的重建。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(FlowAnalysisTestClock.class)
class FlowAnalysisConsistencyIntegrationTest extends FlowAnalysisTestSupport {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresContainer.register(registry);
    }

    @MockitoSpyBean
    EventStore flowEvents;

    @MockitoSpyBean
    BoardActivityLogStore activityLog;

    @Autowired
    FlowAnalysisTestClock.AdjustableTime flowTime;

    @Autowired
    BoardActivityProjection activityProjection;

    @AfterEach
    void restore() {
        reset(flowEvents, activityLog);
        flowTime.reset();
    }

    @Test
    void 情況1_看板上操作後_看板動態已補到最新_同步一次後流程分析看得到() {
        long before = activityLog.checkpoint();
        String board = createBoard("情況1");
        String todo = addColumn(board, "待辦");
        addCard(board, todo, "串接金流");

        assertThat(activityLog.activitiesAfter(before, 500))
                .as("看板指令回覆成功時，看板動態紀錄已補到最新（同一個請求）")
                .extracting(BoardActivity::kind, BoardActivity::boardId)
                .contains(tuple(BOARD_CREATED, board), tuple(COLUMN_ADDED, board), tuple(CARD_ADDED, board));
        assertThat(viewFlow(board).status()).as("流程分析還沒同步").isEqualTo(404);

        sync();

        Reply view = viewFlow(board);
        assertThat(view.status()).isEqualTo(200);
        assertThat(columns(view)).containsExactly("待辦:1");
    }

    @Test
    void 情況2_改設定後立刻查詢_看得到重算後的數字() {
        String board = createBoard("情況2");
        String doing = addColumn(board, "進行中");
        String done = addColumn(board, "完成");
        String card = addCard(board, doing, "串接金流");
        moveCard(board, card, done);
        sync();
        assertThat(viewFlow(board).text("deliverySummary")).isEqualTo("最近 8 週沒有完成的工作項目");
        assertThat(setStage(board, doing, "IN_PROGRESS").status()).isEqualTo(200);

        Reply changed = setStage(board, done, "DONE");
        Reply view = viewFlow(board);

        assertThat(changed.status()).isEqualTo(200);
        assertThat(view.text("deliverySummary")).as("指令回覆時，整段歷史已在同一個指令內重算")
                .startsWith("最近 8 週：完成 1 張");
    }

    @Test
    void 情況3_待滿門檻_檢查後列入卡住_換欄後下一次同步就移出() {
        String board = createBoard("情況3");
        String coding = addColumn(board, "開發中");
        String review = addColumn(board, "審查中");
        String card = addCard(board, coding, "串接金流");
        sync();
        setStage(board, coding, "IN_PROGRESS");
        setStage(board, review, "IN_PROGRESS");
        assertThat(setThreshold(board, 5).status()).isEqualTo(200);
        flowTime.advance(Duration.ofDays(6));

        assertThat(work.checkStuckWorkItems(board).outcome()).isEqualTo(CommandOutcome.SUCCEEDED);
        assertThat(stuckTitles(viewFlow(board))).as("待滿 5 天，定時檢查後列入卡住").containsExactly("串接金流");

        moveCard(board, card, review);
        sync();

        assertThat(stuckTitles(viewFlow(board))).as("換欄後下一次同步就移出").isEmpty();
    }

    @Test
    void 情況4_背景同步失敗_之前的數字照常_看板管理照常_下一輪補上() {
        String board = createBoard("情況4");
        String todo = addColumn(board, "待辦");
        String doing = addColumn(board, "進行中");
        String card = addCard(board, todo, "串接金流");
        sync();
        assertThat(columns(viewFlow(board))).containsExactly("待辦:1", "進行中:0");
        // 流程分析的事件紀錄故障一次，之後恢復正常
        doThrow(new IllegalStateException("流程分析的事件紀錄故障")).doCallRealMethod()
                .when(flowEvents).append(any(), anyLong(), any());

        Reply moved = moveCard(board, card, doing);
        sync();

        assertThat(moved.status()).as("看板管理照常運作").isEqualTo(200);
        assertThat(columns(viewFlow(board))).as("停在出錯的那一筆，之前的數字照常顯示")
                .containsExactly("待辦:1", "進行中:0");

        sync();

        assertThat(columns(viewFlow(board))).as("下一輪補上").containsExactly("待辦:0", "進行中:1");
    }

    @Test
    void 情況4b_看板動態紀錄沒更新成功_看板指令照樣成功_下一次看板指令後補上() {
        String board = createBoard("情況4b");
        addColumn(board, "待辦");
        sync();
        // 看板動態紀錄寫入失敗一次，之後恢復正常
        doThrow(new IllegalStateException("看板動態紀錄故障")).doCallRealMethod().when(activityLog).append(any());

        Reply renamed = put("/api/boards/" + board + "/name", "{\"name\":\"改名後\"}");
        sync();

        assertThat(renamed.status()).as("事件已經存好，看板指令照樣成功").isEqualTo(200);
        assertThat(viewFlow(board).text("boardName")).as("看板動態紀錄沒補上，流程分析讀不到新動態").isEqualTo("情況4b");

        addColumn(board, "完成");
        sync();

        Reply view = viewFlow(board);
        assertThat(view.text("boardName")).as("下一次看板指令成功後補上，再照常同步").isEqualTo("改名後");
        assertThat(columns(view)).containsExactly("待辦:0", "完成:0");
    }

    @Test
    void 情況5_背景同步碰到版本衝突_重試後成功_使用者碰到版本衝突_回覆409() {
        String board = createBoard("情況5");
        addColumn(board, "待辦");
        sync();
        clearInvocations(flowEvents);
        doThrow(new ConcurrentUpdateException("board-flow-" + board, 2)).doCallRealMethod()
                .when(flowEvents).append(any(), anyLong(), any());

        addColumn(board, "完成");
        sync();

        verify(flowEvents, times(2).description("第一次碰到版本衝突，立即重試一次就成功")).append(any(), anyLong(), any());
        assertThat(columns(viewFlow(board))).as("背景那邊自動重試").containsExactly("待辦:0", "完成:0");

        doThrow(new ConcurrentUpdateException("board-flow-" + board, 3)).doCallRealMethod()
                .when(flowEvents).append(any(), anyLong(), any());

        Reply conflict = setThreshold(board, 5);

        assertThat(conflict.status()).isEqualTo(409);
        assertThat(conflict.text("message")).isEqualTo("流程設定已被更新，請重新整理後再試");
    }

    @Test
    void 看板動態紀錄清空重建後_位置不變() {
        String board = createBoard("重建");
        addColumn(board, "待辦");
        List<BoardActivity> before = activityLog.activitiesAfter(0, Integer.MAX_VALUE);

        activityProjection.rebuild();

        assertThat(activityLog.activitiesAfter(0, Integer.MAX_VALUE)).isEqualTo(before);
    }
}
