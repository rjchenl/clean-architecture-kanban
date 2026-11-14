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

import java.util.ArrayList;
import java.util.List;
import kanban.boardmanagement.adapters.eventstore.InMemoryEventStore;
import kanban.boardmanagement.adapters.eventstore.UnknownEventTypeException;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardRenamed;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.usecases.eventfeed.EventFeed;
import kanban.boardmanagement.usecases.eventfeed.RecordedEvent;
import kanban.boardmanagement.usecases.queries.listboards.BoardSummary;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.CardSnapshot;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;
import org.junit.jupiter.api.Test;

/** CQRS-5（冪等）、ES-8（可重建）。 */
class BoardProjectionTest {

    private static final String STREAM = "board-board-1";

    private final InMemoryEventStore events = new InMemoryEventStore();
    private final InMemoryReadModelStore store = new InMemoryReadModelStore();
    private final BoardProjection projection = new BoardProjection(events, store);

    private void givenStandardEvents() {
        events.append(STREAM, 0, List.of(
                created(), column(TODO, "待辦"), column(DOING, "進行中"),
                card(C1, TODO, "第一張"), card(C2, TODO, "第二張"), moved(C1, TODO, DOING, 0, T1)));
    }

    private List<String> cardIdsIn(int columnIndex) {
        return store.boardById("board-1").orElseThrow().columns().get(columnIndex).cards().stream()
                .map(CardSnapshot::cardId).toList();
    }

    @Test
    void catchUp後讀取模型反映全部事件() {
        givenStandardEvents();

        projection.catchUp();

        assertThat(store.checkpoint()).isEqualTo(6);
        assertThat(store.allBoards()).containsExactly(new BoardSummary("board-1", "產品開發", false));
        assertThat(cardIdsIn(0)).containsExactly("card-2");
        assertThat(cardIdsIn(1)).containsExactly("card-1");
        assertThat(store.cardHistory("board-1", "card-1").orElseThrow().entries()).hasSize(2);
        assertThat(store.cardHistory("board-1", "card-2").orElseThrow().entries()).hasSize(1);
    }

    @Test
    void 沒有事件時catchUp_什麼都不做() {
        projection.catchUp();

        assertThat(store.checkpoint()).isZero();
        assertThat(store.allBoards()).isEmpty();
    }

    @Test
    void catchUp連續呼叫兩次_結果不變() {
        givenStandardEvents();
        projection.catchUp();
        BoardSnapshot board = store.boardById("board-1").orElseThrow();
        CardHistory history = store.cardHistory("board-1", "card-1").orElseThrow();

        projection.catchUp();

        assertThat(store.boardById("board-1")).contains(board);
        assertThat(store.cardHistory("board-1", "card-1")).contains(history);
        assertThat(store.checkpoint()).isEqualTo(6);
    }

    @Test
    void 後來又有新事件_只補新的() {
        givenStandardEvents();
        projection.catchUp();
        events.append(STREAM, 6, List.of(new BoardRenamed(BOARD, new BoardName("新產品"), T1)));

        projection.catchUp();

        assertThat(store.boardById("board-1").orElseThrow().name()).isEqualTo("新產品");
        assertThat(store.checkpoint()).isEqualTo(7);
        assertThat(store.cardHistory("board-1", "card-1").orElseThrow().entries()).as("舊事件沒有被重複處理").hasSize(2);
    }

    @Test
    void 已處理過的事件再送一次_被略過_結果不變() {
        // 故意做一個會重送舊事件的 EventFeed：第一次給全部 6 筆，之後每次都再送第 5、6 筆（新增 card-2、移動 card-1）。
        // 沒有略過的話，card-2 會被加兩次、card-1 會被移兩次。
        givenStandardEvents();
        EventFeed redelivering = (position, maxCount) ->
                position == 0 ? events.readAfter(0, maxCount) : events.readAfter(4, maxCount);
        BoardProjection projectionWithRedelivery = new BoardProjection(redelivering, store);

        projectionWithRedelivery.catchUp();
        projectionWithRedelivery.catchUp();

        assertThat(store.checkpoint()).isEqualTo(6);
        assertThat(cardIdsIn(0)).containsExactly("card-2");
        assertThat(cardIdsIn(1)).containsExactly("card-1");
        assertThat(store.cardHistory("board-1", "card-1").orElseThrow().entries()).hasSize(2);
    }

    @Test
    void 重建後與重建前完全相同() {
        givenStandardEvents();
        events.append("board-board-2", 0, List.of(
                new kanban.boardmanagement.entities.board.BoardCreated(
                        new kanban.boardmanagement.entities.board.BoardId("board-2"), new BoardName("第二個看板"), T1)));
        projection.catchUp();
        List<BoardSummary> boards = store.allBoards();
        BoardSnapshot board = store.boardById("board-1").orElseThrow();
        CardHistory first = store.cardHistory("board-1", "card-1").orElseThrow();
        CardHistory second = store.cardHistory("board-1", "card-2").orElseThrow();
        long checkpoint = store.checkpoint();

        projection.rebuild();

        assertThat(store.allBoards()).isEqualTo(boards).hasSize(2);
        assertThat(store.boardById("board-1")).contains(board);
        assertThat(store.cardHistory("board-1", "card-1")).contains(first);
        assertThat(store.cardHistory("board-1", "card-2")).contains(second);
        assertThat(store.checkpoint()).isEqualTo(checkpoint);
    }

    @Test
    void 重建會丟掉讀取模型裡原本的東西() {
        givenStandardEvents();
        store.inExclusiveTransaction(() -> {
            store.saveBoard(new BoardSnapshot("ghost", "不存在於事件的看板", false, List.of()));
            store.advanceCheckpoint(6);
        });

        projection.rebuild();

        assertThat(store.boardById("ghost")).isEmpty();
        assertThat(store.allBoards()).containsExactly(new BoardSummary("board-1", "產品開發", false));
    }

    @Test
    void 遇到處理不了的事件_丟例外且checkpoint停在前一筆() {
        // 前兩筆正常；再往後讀時事件庫回報「這一版程式不認得的事件」
        EventFeed failingAfterTwo = (position, maxCount) -> {
            if (position >= 2) {
                throw new UnknownEventTypeException("BoardExploded", 1);
            }
            return List.of(new RecordedEvent(1, created()), new RecordedEvent(2, column(TODO, "待辦")));
        };
        BoardProjection failing = new BoardProjection(failingAfterTwo, store);

        assertThatThrownBy(failing::catchUp).isInstanceOf(UnknownEventTypeException.class);

        assertThat(store.checkpoint()).isEqualTo(2);
        assertThat(store.boardById("board-1").orElseThrow().columns()).hasSize(1);
    }

    @Test
    void 套用到一半失敗_那一筆的寫入不保留() {
        events.append(STREAM, 0, List.of(created(), column(TODO, "待辦"), column(DOING, "進行中")));
        FailingStore failingStore = new FailingStore(store, 3);
        BoardProjection failing = new BoardProjection(events, failingStore);

        assertThatThrownBy(failing::catchUp).isInstanceOf(IllegalStateException.class);

        assertThat(store.checkpoint()).isEqualTo(2);
        assertThat(store.boardById("board-1").orElseThrow().columns()).as("第 3 筆（新增「進行中」）沒有留下").hasSize(1);

        // 故障排除後再補一次，就補齊了
        projection.catchUp();
        assertThat(store.checkpoint()).isEqualTo(3);
        assertThat(store.boardById("board-1").orElseThrow().columns()).hasSize(2);
    }

    @Test
    void 超過200筆事件_全部補完() {
        List<BoardEvent> many = new ArrayList<>();
        many.add(created());
        many.add(column(TODO, "待辦"));
        for (int i = 0; i < 450; i++) {
            many.add(new BoardRenamed(BOARD, new BoardName("第 " + i + " 次改名"), T1));
        }
        events.append(STREAM, 0, many);

        projection.catchUp();

        assertThat(store.checkpoint()).isEqualTo(452);
        assertThat(store.boardById("board-1").orElseThrow().name()).isEqualTo("第 449 次改名");
    }

    /** 測試用：在第 N 次儲存看板時故障。其餘都轉給真正的儲存。 */
    private static final class FailingStore implements ReadModelStore {
        private final ReadModelStore delegate;
        private final int failOnSave;
        private int saves;

        FailingStore(ReadModelStore delegate, int failOnSave) {
            this.delegate = delegate;
            this.failOnSave = failOnSave;
        }

        @Override
        public void saveBoard(BoardSnapshot board) {
            saves++;
            delegate.saveBoard(board);
            if (saves == failOnSave) {
                throw new IllegalStateException("讀取模型故障");
            }
        }

        @Override public long checkpoint() { return delegate.checkpoint(); }
        @Override public void inExclusiveTransaction(Runnable work) { delegate.inExclusiveTransaction(work); }
        @Override public void saveCardHistory(String boardId, CardHistory history) { delegate.saveCardHistory(boardId, history); }
        @Override public void advanceCheckpoint(long position) { delegate.advanceCheckpoint(position); }
        @Override public void reset() { delegate.reset(); }
        @Override public java.util.Optional<BoardSnapshot> boardById(String boardId) { return delegate.boardById(boardId); }
        @Override public List<BoardSummary> allBoards() { return delegate.allBoards(); }
        @Override public java.util.Optional<CardHistory> cardHistory(String boardId, String cardId) { return delegate.cardHistory(boardId, cardId); }
    }
}
