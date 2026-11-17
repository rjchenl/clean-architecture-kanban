package kanban.flowanalysis.frameworks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import javax.sql.DataSource;
import kanban.flowanalysis.adapters.eventstore.BoardFlowEventJson;
import kanban.flowanalysis.adapters.eventstore.PostgresEventStore;
import kanban.flowanalysis.adapters.projection.FlowProjection;
import kanban.flowanalysis.adapters.projection.PostgresReadModelStore;
import kanban.flowanalysis.adapters.projection.WeekCalendar;
import kanban.flowanalysis.entities.boardflow.ActivityPosition;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.ColumnStageSet;
import kanban.flowanalysis.entities.boardflow.DwellTime;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import kanban.flowanalysis.entities.boardflow.FlowColumnAdded;
import kanban.flowanalysis.entities.boardflow.FlowColumnId;
import kanban.flowanalysis.entities.boardflow.FlowColumnName;
import kanban.flowanalysis.entities.boardflow.LeadTime;
import kanban.flowanalysis.entities.boardflow.Stage;
import kanban.flowanalysis.entities.boardflow.WorkItemAdded;
import kanban.flowanalysis.entities.boardflow.WorkItemFlowResultChanged;
import kanban.flowanalysis.entities.boardflow.WorkItemId;
import kanban.flowanalysis.entities.boardflow.WorkItemMoved;
import kanban.flowanalysis.entities.boardflow.WorkItemTitle;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.Delivery;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 流程分析的 Projection 接真的 PostgreSQL：從頭補齊、重建後與重建前完全相同（ES-8）。 */
class PostgresProjectionTest {

    private static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");
    private static final Instant AS_OF = Instant.parse("2026-11-18T04:00:00Z");
    private static final BoardFlowId FLOW = new BoardFlowId("board-1");
    private static final FlowColumnId TODO = new FlowColumnId("col-todo");
    private static final FlowColumnId DONE = new FlowColumnId("col-done");
    private static final WorkItemId CARD = new WorkItemId("card-1");

    private PostgresEventStore events;
    private PostgresReadModelStore store;
    private FlowProjection projection;

    @BeforeEach
    void freshDatabase() {
        DataSource dataSource = FlowAnalysisTestDatabase.freshDatabase();
        events = new PostgresEventStore(dataSource, new BoardFlowEventJson());
        store = new PostgresReadModelStore(dataSource, new WeekCalendar(ZoneId.of("Asia/Taipei")));
        projection = new FlowProjection(events, store);
        Instant day1 = T0.plus(Duration.ofDays(1));
        events.append("board-flow-board-1", 0, List.of(
                new BoardFlowCreated(FLOW, new ActivityPosition(1), new FlowBoardName("產品開發"), T0),
                new FlowColumnAdded(FLOW, new ActivityPosition(2), TODO, new FlowColumnName("待辦"), Stage.UNCLASSIFIED, T0),
                new FlowColumnAdded(FLOW, new ActivityPosition(3), DONE, new FlowColumnName("完成"), Stage.UNCLASSIFIED, T0),
                new ColumnStageSet(FLOW, TODO, Stage.NOT_STARTED, T0),
                new ColumnStageSet(FLOW, DONE, Stage.DONE, T0),
                new WorkItemAdded(FLOW, new ActivityPosition(4), CARD, TODO, new WorkItemTitle("串接金流"), T0),
                new WorkItemMoved(FLOW, new ActivityPosition(5), CARD, TODO, DONE, new DwellTime(Duration.ofDays(1)), day1),
                new WorkItemFlowResultChanged(FLOW, CARD, day1, day1, new LeadTime(Duration.ZERO), day1)));
    }

    private FlowMetrics metrics() {
        return store.flowMetrics("board-1", AS_OF).orElseThrow();
    }

    @Test
    void 從頭補齊() {
        projection.catchUp();

        assertThat(store.checkpoint()).isEqualTo(8);
        assertThat(metrics().columns()).extracting(column -> column.name() + ":" + column.workItemCount())
                .containsExactly("待辦:0", "完成:1");
        assertThat(metrics().delivery()).isEqualTo(new Delivery(1, 0L, 0L));
        assertThat(store.allBoardFlowIds()).containsExactly("board-1");
    }

    @Test
    void 重建後與重建前完全相同() {
        projection.catchUp();
        FlowMetrics before = metrics();

        projection.rebuild();

        assertThat(metrics()).isEqualTo(before);
        assertThat(store.checkpoint()).isEqualTo(8);
    }
}
