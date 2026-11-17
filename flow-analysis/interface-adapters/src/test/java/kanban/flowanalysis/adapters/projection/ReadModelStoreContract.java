package kanban.flowanalysis.adapters.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.ColumnEntry;
import kanban.flowanalysis.adapters.projection.FlowBoardDocument.WorkItemEntry;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.ColumnMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.Delivery;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.StuckWorkItem;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.WeeklyCompletion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 流程分析讀取模型的契約（CA-6）：記憶體版與 PostgreSQL 版各跑一次。統計方式 S1、S2 在這裡守（設計第 7.4 節）。
 * 子類別在同一支測試裡每次 {@link #store()} 都要回同一個儲存，並用 {@link #CALENDAR} 建立它。
 */
public abstract class ReadModelStoreContract {

    protected static final WeekCalendar CALENDAR = new WeekCalendar(ZoneId.of("Asia/Taipei"));
    /** 台北時間 2026-11-18（週三）12:00；本週從 2026-11-16（週一）00:00 起。 */
    protected static final Instant AS_OF = Instant.parse("2026-11-18T04:00:00Z");
    protected static final Instant THIS_WEEK = Instant.parse("2026-11-15T16:00:00Z");
    protected static final Instant LAST_WEEK = Instant.parse("2026-11-08T16:00:00Z");

    protected abstract ReadModelStore store();

    protected static FlowBoardDocument board(String id, String name) {
        return new FlowBoardDocument(id, name, false, null, List.of(), List.of());
    }

    private FlowMetrics metricsOf(String id) {
        return store().flowMetrics(id, AS_OF).orElseThrow();
    }

    private void write(Runnable work) {
        store().inExclusiveTransaction(work);
    }

    @BeforeEach
    void startEmpty() {
        store().inExclusiveTransaction(store()::reset);
    }

    // ---------------------------------------------------------------- 看板流程文件

    @Test
    void 一開始是空的_checkpoint為0() {
        assertThat(store().flowMetrics("board-1", AS_OF)).isEmpty();
        assertThat(store().allBoardFlowIds()).isEmpty();
        assertThat(store().checkpoint()).isZero();
    }

    @Test
    void 存入的看板流程讀得回來() {
        FlowBoardDocument doc = new FlowBoardDocument("board-1", "產品開發", true, 5,
                List.of(new ColumnEntry("col-todo", "待辦", "NOT_STARTED", false, 0, 0)),
                List.of(new WorkItemEntry("card-1", "串接金流", "col-todo", false, null)));

        write(() -> store().saveBoardFlow(doc));

        assertThat(store().boardFlowDocument("board-1")).contains(doc);
        FlowMetrics metrics = metricsOf("board-1");
        assertThat(metrics.boardName()).isEqualTo("產品開發");
        assertThat(metrics.archived()).isTrue();
        assertThat(metrics.stuckThresholdDays()).isEqualTo(5);
    }

    @Test
    void 同一個看板流程再存一次_覆蓋舊的() {
        write(() -> {
            store().saveBoardFlow(board("board-1", "舊名稱"));
            store().saveBoardFlow(board("board-1", "新名稱"));
        });

        assertThat(metricsOf("board-1").boardName()).isEqualTo("新名稱");
        assertThat(store().allBoardFlowIds()).containsExactly("board-1");
    }

    @Test
    void 看板流程清單依第一次出現的順序_更新不改變順序() {
        write(() -> {
            store().saveBoardFlow(board("board-2", "乙"));
            store().saveBoardFlow(board("board-1", "甲"));
            store().saveBoardFlow(board("board-2", "乙改"));
        });

        assertThat(store().allBoardFlowIds()).containsExactly("board-2", "board-1");
    }

    // ---------------------------------------------------------------- S2：各欄張數、停留平均、卡住清單

    @Test
    void 各欄目前張數與停留平均_平均四捨五入到整數秒_刪除的欄不顯示() {
        FlowBoardDocument doc = new FlowBoardDocument("board-1", "產品開發", false, null,
                List.of(new ColumnEntry("col-doing", "進行中", "IN_PROGRESS", false, 3, 2),
                        new ColumnEntry("col-todo", "待辦", "NOT_STARTED", false, 0, 0),
                        new ColumnEntry("col-old", "舊欄", "DONE", true, 100, 1)),
                List.of(new WorkItemEntry("card-1", "一", "col-doing", false, null),
                        new WorkItemEntry("card-2", "二", "col-doing", false, null),
                        new WorkItemEntry("card-3", "三", "col-todo", false, null)));

        write(() -> store().saveBoardFlow(doc));

        assertThat(metricsOf("board-1").columns()).containsExactly(
                new ColumnMetrics("col-doing", "進行中", "IN_PROGRESS", 2, 2L),
                new ColumnMetrics("col-todo", "待辦", "NOT_STARTED", 1, null));
    }

    @Test
    void 卡住清單附所在欄名稱() {
        Instant since = Instant.parse("2026-11-10T01:00:00Z");
        FlowBoardDocument doc = new FlowBoardDocument("board-1", "產品開發", false, 5,
                List.of(new ColumnEntry("col-doing", "進行中", "IN_PROGRESS", false, 0, 0)),
                List.of(new WorkItemEntry("card-1", "串接金流", "col-doing", true, since),
                        new WorkItemEntry("card-2", "寫文件", "col-doing", false, null)));

        write(() -> store().saveBoardFlow(doc));

        assertThat(metricsOf("board-1").stuckWorkItems())
                .containsExactly(new StuckWorkItem("card-1", "串接金流", "進行中", since));
    }

    // ---------------------------------------------------------------- S1、S2：最近 8 週

    @Test
    void 最近8週含本週_由舊到新_每週完成數() {
        write(() -> {
            store().saveBoardFlow(board("board-1", "產品開發"));
            store().saveCompletion("board-1", "card-1", THIS_WEEK.plusSeconds(60), 3_600);
            store().saveCompletion("board-1", "card-2", THIS_WEEK.plus(Duration.ofDays(1)), 3_600);
            store().saveCompletion("board-1", "card-3", LAST_WEEK.plusSeconds(1), 3_600);
        });

        List<WeeklyCompletion> weeks = metricsOf("board-1").recentWeeks();

        assertThat(weeks).hasSize(8);
        assertThat(weeks.get(7)).isEqualTo(new WeeklyCompletion(THIS_WEEK, 2));
        assertThat(weeks.get(6)).isEqualTo(new WeeklyCompletion(LAST_WEEK, 1));
        assertThat(weeks.get(0)).isEqualTo(new WeeklyCompletion(Instant.parse("2026-09-27T16:00:00Z"), 0));
    }

    @Test
    void 週一0點分界_台北時區() {
        write(() -> {
            store().saveBoardFlow(board("board-1", "產品開發"));
            // 台北時間週日 23:59 算上一週；週一 00:00 算這一週
            store().saveCompletion("board-1", "card-1", Instant.parse("2026-11-15T15:59:00Z"), 60);
            store().saveCompletion("board-1", "card-2", Instant.parse("2026-11-15T16:00:00Z"), 60);
        });

        List<WeeklyCompletion> weeks = metricsOf("board-1").recentWeeks();

        assertThat(weeks.get(6).completedCount()).isEqualTo(1);
        assertThat(weeks.get(7).completedCount()).isEqualTo(1);
    }

    @Test
    void 平均與最長只算最近8週內完成的_平均四捨五入到整數秒() {
        write(() -> {
            store().saveBoardFlow(board("board-1", "產品開發"));
            store().saveCompletion("board-1", "card-1", THIS_WEEK, 1);
            store().saveCompletion("board-1", "card-2", LAST_WEEK, 2);
            // 9 週前完成的不算
            store().saveCompletion("board-1", "card-3", THIS_WEEK.minus(Duration.ofDays(63)), 9_999);
        });

        assertThat(metricsOf("board-1").delivery()).isEqualTo(new Delivery(2, 2L, 2L));
    }

    @Test
    void 沒有完成的_平均與最長是null() {
        write(() -> store().saveBoardFlow(board("board-1", "產品開發")));

        assertThat(metricsOf("board-1").delivery()).isEqualTo(new Delivery(0, null, null));
    }

    @Test
    void 移出完成後_那一筆不再算() {
        write(() -> {
            store().saveBoardFlow(board("board-1", "產品開發"));
            store().saveCompletion("board-1", "card-1", THIS_WEEK, 100);
        });

        write(() -> store().removeCompletion("board-1", "card-1"));

        assertThat(metricsOf("board-1").delivery()).isEqualTo(new Delivery(0, null, null));
        assertThat(metricsOf("board-1").recentWeeks().get(7).completedCount()).isZero();
    }

    @Test
    void 同一個工作項目重新完成_覆蓋舊的完成紀錄() {
        write(() -> {
            store().saveBoardFlow(board("board-1", "產品開發"));
            store().saveCompletion("board-1", "card-1", LAST_WEEK, 100);
            store().saveCompletion("board-1", "card-1", THIS_WEEK, 300);
        });

        FlowMetrics metrics = metricsOf("board-1");
        assertThat(metrics.delivery()).isEqualTo(new Delivery(1, 300L, 300L));
        assertThat(metrics.recentWeeks().get(6).completedCount()).isZero();
        assertThat(metrics.recentWeeks().get(7).completedCount()).isEqualTo(1);
    }

    @Test
    void 不同看板流程的完成紀錄互不影響() {
        write(() -> {
            store().saveBoardFlow(board("board-1", "甲"));
            store().saveBoardFlow(board("board-2", "乙"));
            store().saveCompletion("board-2", "card-1", THIS_WEEK, 100);
        });

        assertThat(metricsOf("board-1").delivery().completedCount()).isZero();
        assertThat(metricsOf("board-2").delivery().completedCount()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- checkpoint 與交易

    @Test
    void checkpoint可以前進() {
        write(() -> store().advanceCheckpoint(7));

        assertThat(store().checkpoint()).isEqualTo(7);
    }

    @Test
    void 交易內讀得到自己剛寫的資料() {
        write(() -> {
            store().saveBoardFlow(board("board-1", "產品開發"));
            store().advanceCheckpoint(1);
            assertThat(store().boardFlowDocument("board-1")).isPresent();
            assertThat(store().checkpoint()).isEqualTo(1);
        });
    }

    @Test
    void reset後全部清空且checkpoint為0() {
        write(() -> {
            store().saveBoardFlow(board("board-1", "產品開發"));
            store().saveCompletion("board-1", "card-1", THIS_WEEK, 100);
            store().advanceCheckpoint(3);
        });

        write(store()::reset);

        assertThat(store().allBoardFlowIds()).isEmpty();
        assertThat(store().flowMetrics("board-1", AS_OF)).isEmpty();
        assertThat(store().checkpoint()).isZero();
    }

    @Test
    void 交易內丟例外_寫入與checkpoint都不保留() {
        assertThatThrownBy(() -> write(() -> {
            store().saveBoardFlow(board("board-1", "產品開發"));
            store().saveCompletion("board-1", "card-1", THIS_WEEK, 100);
            store().advanceCheckpoint(1);
            throw new IllegalStateException("中途失敗");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store().allBoardFlowIds()).isEmpty();
        assertThat(store().checkpoint()).isZero();
    }

    @Test
    void 寫入方法只能在交易內呼叫() {
        assertThatThrownBy(() -> store().saveBoardFlow(board("board-1", "產品開發"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store().saveCompletion("board-1", "card-1", THIS_WEEK, 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store().removeCompletion("board-1", "card-1")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store().advanceCheckpoint(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store().reset()).isInstanceOf(IllegalStateException.class);
    }
}
