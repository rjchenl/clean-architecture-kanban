package kanban.flowanalysis.adapters.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import kanban.flowanalysis.adapters.eventstore.InMemoryEventStore;
import kanban.flowanalysis.adapters.eventstore.UnknownEventTypeException;
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
import kanban.flowanalysis.usecases.eventfeed.EventFeed;
import kanban.flowanalysis.usecases.eventfeed.RecordedEvent;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.Delivery;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetrics;
import org.junit.jupiter.api.Test;

/** 流程分析的 Projection：CQRS-5（冪等）、ES-8（可重建）。 */
class FlowProjectionTest {

    private static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");
    private static final Instant AS_OF = Instant.parse("2026-11-18T04:00:00Z");
    private static final BoardFlowId FLOW = new BoardFlowId("board-1");
    private static final FlowColumnId TODO = new FlowColumnId("col-todo");
    private static final FlowColumnId DONE = new FlowColumnId("col-done");
    private static final WorkItemId CARD = new WorkItemId("card-1");
    private static final String STREAM = "board-flow-board-1";

    private final InMemoryEventStore events = new InMemoryEventStore();
    private final InMemoryReadModelStore store = new InMemoryReadModelStore(new WeekCalendar(ZoneId.of("Asia/Taipei")));
    private final FlowProjection projection = new FlowProjection(events, store);

    private static ActivityPosition at(long position) {
        return new ActivityPosition(position);
    }

    /** 待辦（未開始）、完成兩欄；card-1 從待辦換到完成（第 1 天完成，交付 1 天）。 */
    private void givenCompletedCard() {
        events.append(STREAM, 0, List.of(
                new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0),
                new FlowColumnAdded(FLOW, at(2), TODO, new FlowColumnName("待辦"), Stage.UNCLASSIFIED, T0),
                new FlowColumnAdded(FLOW, at(3), DONE, new FlowColumnName("完成"), Stage.UNCLASSIFIED, T0),
                new ColumnStageSet(FLOW, TODO, Stage.NOT_STARTED, T0),
                new ColumnStageSet(FLOW, DONE, Stage.DONE, T0),
                new WorkItemAdded(FLOW, at(4), CARD, TODO, new WorkItemTitle("串接金流"), T0),
                new WorkItemMoved(FLOW, at(5), CARD, TODO, DONE, new DwellTime(Duration.ofDays(1)), T0.plus(Duration.ofDays(1))),
                new WorkItemFlowResultChanged(FLOW, CARD, T0.plus(Duration.ofDays(1)), T0.plus(Duration.ofDays(1)),
                        new LeadTime(Duration.ZERO), T0.plus(Duration.ofDays(1)))));
    }

    private FlowMetrics metrics() {
        return store.flowMetrics("board-1", AS_OF).orElseThrow();
    }

    @Test
    void 從頭補齊() {
        givenCompletedCard();

        projection.catchUp();

        assertThat(store.checkpoint()).isEqualTo(8);
        FlowMetrics metrics = metrics();
        assertThat(metrics.columns()).extracting(column -> column.name() + ":" + column.stage() + ":" + column.workItemCount())
                .containsExactly("待辦:NOT_STARTED:0", "完成:DONE:1");
        assertThat(metrics.columns().getFirst().averageDwellSeconds()).isEqualTo(86_400L);
        assertThat(metrics.delivery()).isEqualTo(new Delivery(1, 0L, 0L));
        assertThat(metrics.recentWeeks().getLast().completedCount()).isEqualTo(1);
    }

    @Test
    void 不再完成_完成紀錄被移除() {
        givenCompletedCard();
        events.append(STREAM, 8, List.of(
                new WorkItemMoved(FLOW, at(6), CARD, DONE, TODO, new DwellTime(Duration.ofHours(1)), T0.plus(Duration.ofDays(2))),
                new WorkItemFlowResultChanged(FLOW, CARD, T0.plus(Duration.ofDays(1)), null, null, T0.plus(Duration.ofDays(2)))));

        projection.catchUp();

        assertThat(metrics().delivery()).isEqualTo(new Delivery(0, null, null));
    }

    @Test
    void 已處理過的事件再送一次_被略過_結果不變() {
        givenCompletedCard();
        projection.catchUp();
        FlowMetrics before = metrics();
        List<RecordedEvent> all = events.readAfter(0, 100);
        EventFeed replaying = (position, maxCount) -> all;

        new FlowProjection(replaying, store).catchUp();

        assertThat(metrics()).isEqualTo(before);
        assertThat(store.checkpoint()).isEqualTo(8);
    }

    @Test
    void 重建後與重建前完全相同() {
        givenCompletedCard();
        projection.catchUp();
        FlowMetrics before = metrics();

        projection.rebuild();

        assertThat(metrics()).isEqualTo(before);
        assertThat(store.checkpoint()).isEqualTo(8);
    }

    @Test
    void 遇到處理不了的事件_丟例外且checkpoint停在前一筆() {
        RecordedEvent first = new RecordedEvent(1, new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0));
        EventFeed breaksAfterFirst = (position, maxCount) -> {
            if (position == 0) {
                return List.of(first);
            }
            throw new UnknownEventTypeException("BoardFlowExploded", 1);
        };

        assertThatThrownBy(new FlowProjection(breaksAfterFirst, store)::catchUp)
                .isInstanceOf(UnknownEventTypeException.class);
        assertThat(store.checkpoint()).isEqualTo(1);
        assertThat(store.allBoardFlowIds()).containsExactly("board-1");
    }
}
