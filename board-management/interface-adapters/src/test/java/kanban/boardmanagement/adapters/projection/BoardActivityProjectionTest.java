package kanban.boardmanagement.adapters.projection;

import static kanban.boardmanagement.adapters.projection.ProjectionEvents.BOARD;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.C1;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.C2;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.DOING;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.T1;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.TODO;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.card;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.column;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.created;
import static kanban.boardmanagement.adapters.projection.ProjectionEvents.moved;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import kanban.boardmanagement.adapters.eventstore.InMemoryEventStore;
import kanban.boardmanagement.adapters.eventstore.UnknownEventTypeException;
import kanban.boardmanagement.entities.board.ColumnWipLimitSet;
import kanban.boardmanagement.entities.board.WipLimit;
import kanban.boardmanagement.usecases.eventfeed.EventFeed;
import kanban.boardmanagement.usecases.eventfeed.RecordedEvent;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind;
import org.junit.jupiter.api.Test;

/** 看板動態的 Projection：自己的處理位置，所以上線時從第一筆事件補齊（CQRS-5 冪等、ES-8 可重建）。 */
class BoardActivityProjectionTest {

    private static final String STREAM = "board-board-1";

    private final InMemoryEventStore events = new InMemoryEventStore();
    private final InMemoryBoardActivityLog log = new InMemoryBoardActivityLog();
    private final BoardActivityProjection projection = new BoardActivityProjection(events, log);

    private List<BoardActivity> allActivities() {
        return log.activitiesAfter(0, 500);
    }

    @Test
    void 從頭補齊_位置等於事件的全域位置() {
        events.append(STREAM, 0, List.of(created(), column(TODO, "待辦"), column(DOING, "進行中"),
                card(C1, TODO, "第一張"), card(C2, TODO, "第二張"), moved(C1, TODO, DOING, 0, T1)));

        projection.catchUp();

        assertThat(allActivities()).extracting(BoardActivity::position).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
        assertThat(allActivities()).extracting(BoardActivity::kind).containsExactly(
                BoardActivityKind.BOARD_CREATED, BoardActivityKind.COLUMN_ADDED, BoardActivityKind.COLUMN_ADDED,
                BoardActivityKind.CARD_ADDED, BoardActivityKind.CARD_ADDED, BoardActivityKind.CARD_MOVED);
        assertThat(log.checkpoint()).isEqualTo(6);
    }

    @Test
    void 不公布的事件_checkpoint照樣前進() {
        events.append(STREAM, 0, List.of(created(), column(TODO, "待辦"),
                new ColumnWipLimitSet(BOARD, TODO, new WipLimit(3), T1)));

        projection.catchUp();

        assertThat(allActivities()).extracting(BoardActivity::position).containsExactly(1L, 2L);
        assertThat(log.checkpoint()).isEqualTo(3);
    }

    @Test
    void 已處理過的事件再送一次_被略過_結果不變() {
        RecordedEvent first = new RecordedEvent(1, created());
        RecordedEvent second = new RecordedEvent(2, column(TODO, "待辦"));
        EventFeed replaying = (position, maxCount) -> List.of(first, second);
        BoardActivityProjection replayed = new BoardActivityProjection(replaying, log);

        replayed.catchUp();
        replayed.catchUp();

        assertThat(allActivities()).extracting(BoardActivity::position).containsExactly(1L, 2L);
        assertThat(log.checkpoint()).isEqualTo(2);
    }

    @Test
    void 重建後與重建前完全相同_位置不變() {
        events.append(STREAM, 0, List.of(created(), column(TODO, "待辦"), card(C1, TODO, "第一張"),
                moved(C1, TODO, TODO, 0, T1)));
        projection.catchUp();
        List<BoardActivity> before = allActivities();

        projection.rebuild();

        assertThat(allActivities()).isEqualTo(before);
        assertThat(log.checkpoint()).isEqualTo(4);
    }

    @Test
    void 遇到處理不了的事件_丟例外且checkpoint停在前一筆() {
        RecordedEvent first = new RecordedEvent(1, created());
        EventFeed breaksAfterFirst = (position, maxCount) -> {
            if (position == 0) {
                return List.of(first);
            }
            throw new UnknownEventTypeException("BoardExploded", 1);
        };
        BoardActivityProjection stuck = new BoardActivityProjection(breaksAfterFirst, log);

        assertThatThrownBy(stuck::catchUp).isInstanceOf(UnknownEventTypeException.class);
        assertThat(log.checkpoint()).isEqualTo(1);
        assertThat(allActivities()).hasSize(1);
    }
}
