package kanban.boardmanagement.frameworks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import kanban.boardmanagement.adapters.eventstore.BoardEventJson;
import kanban.boardmanagement.adapters.eventstore.EventSourcedBoardRepository;
import kanban.boardmanagement.adapters.eventstore.PostgresEventStore;
import kanban.boardmanagement.adapters.projection.BoardProjection;
import kanban.boardmanagement.adapters.projection.PostgresReadModelStore;
import kanban.boardmanagement.entities.board.Board;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.entities.board.CardDescription;
import kanban.boardmanagement.entities.board.CardId;
import kanban.boardmanagement.entities.board.CardTitle;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.entities.board.Position;
import kanban.boardmanagement.entities.board.WipLimit;
import kanban.boardmanagement.usecases.queries.listboards.BoardSummary;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 事件庫、Projection、讀取模型全部接上 PostgreSQL（CQRS-5、ES-8）。 */
class PostgresProjectionTest {

    private static final Instant AT = Instant.parse("2026-11-14T01:00:00Z");

    private PostgresEventStore events;
    private PostgresReadModelStore store;
    private BoardProjection projection;

    @BeforeEach
    void realBoardStoredAsEvents() {
        DataSource dataSource = PostgresTestDatabase.freshDatabase();
        events = new PostgresEventStore(dataSource, new BoardEventJson());
        store = new PostgresReadModelStore(dataSource);
        projection = new BoardProjection(events, store);

        Board board = Board.create(new BoardId("board-1"), new BoardName("產品開發"), AT);
        board.addColumn(new ColumnId("col-todo"), new ColumnName("待辦"), AT);
        board.addColumn(new ColumnId("col-doing"), new ColumnName("進行中"), AT);
        board.setColumnWipLimit(new ColumnId("col-doing"), new WipLimit(2), AT);
        board.addCard(new CardId("card-1"), new ColumnId("col-todo"), new CardTitle("第一張"), new CardDescription("說明"), AT);
        board.addCard(new CardId("card-2"), new ColumnId("col-todo"), new CardTitle("第二張"), new CardDescription(""), AT);
        board.moveCard(new CardId("card-1"), new ColumnId("col-doing"), new Position(0), AT.plusSeconds(60));
        board.renameColumn(new ColumnId("col-todo"), new ColumnName("準備中"), AT.plusSeconds(120));
        new EventSourcedBoardRepository(events).save(board);
    }

    @Test
    void 從資料庫的事件投影出看板現況與卡片歷程() {
        projection.catchUp();

        BoardSnapshot board = store.boardById("board-1").orElseThrow();
        assertThat(board.columns()).extracting(c -> c.name() + ":" + c.cards().size() + ":" + c.wipLimit())
                .containsExactly("準備中:1:null", "進行中:1:2");
        assertThat(store.cardHistory("board-1", "card-1").orElseThrow().entries())
                .extracting(e -> e.kind() + ":" + e.fromColumnName() + "→" + e.toColumnName())
                .containsExactly("ADDED:null→待辦", "MOVED:待辦→進行中");
        assertThat(store.checkpoint()).isEqualTo(8);
    }

    @Test
    void 事件紀錄裡出現不認得的事件_前面的正常事件照樣投影_然後明確報錯停住() throws Exception {
        // 第 9 筆是這一版程式不認得的事件類型（例如較新版本寫入的）
        try (var connection = PostgresTestDatabase.dataSource().getConnection();
             var statement = connection.createStatement()) {
            statement.execute("INSERT INTO board_events (stream_id, stream_version, event_type, schema_version, payload, "
                    + "occurred_at) VALUES ('board-board-1', 9, 'BoardExploded', 1, '{}'::jsonb, now())");
        }

        org.assertj.core.api.Assertions.assertThatThrownBy(projection::catchUp)
                .isInstanceOf(kanban.boardmanagement.adapters.eventstore.UnknownEventTypeException.class)
                .hasMessageContaining("BoardExploded");

        assertThat(store.checkpoint()).as("同一批裡前 8 筆正常事件不受連累").isEqualTo(8);
        assertThat(store.boardById("board-1")).isPresent();

        // 再補一次：仍然停在同一筆、明確報錯，不會默默略過
        org.assertj.core.api.Assertions.assertThatThrownBy(projection::catchUp)
                .isInstanceOf(kanban.boardmanagement.adapters.eventstore.UnknownEventTypeException.class);
        assertThat(store.checkpoint()).isEqualTo(8);
    }

    @Test
    void catchUp兩次結果不變() {
        projection.catchUp();
        BoardSnapshot board = store.boardById("board-1").orElseThrow();
        CardHistory history = store.cardHistory("board-1", "card-1").orElseThrow();

        projection.catchUp();

        assertThat(store.boardById("board-1")).contains(board);
        assertThat(store.cardHistory("board-1", "card-1")).contains(history);
        assertThat(store.checkpoint()).isEqualTo(8);
    }

    @Test
    void 重建後與重建前完全相同() {
        projection.catchUp();
        List<BoardSummary> boards = store.allBoards();
        BoardSnapshot board = store.boardById("board-1").orElseThrow();
        CardHistory first = store.cardHistory("board-1", "card-1").orElseThrow();
        CardHistory second = store.cardHistory("board-1", "card-2").orElseThrow();
        long checkpoint = store.checkpoint();

        projection.rebuild();

        assertThat(store.allBoards()).isEqualTo(boards).hasSize(1);
        assertThat(store.boardById("board-1")).contains(board);
        assertThat(store.cardHistory("board-1", "card-1")).contains(first);
        assertThat(store.cardHistory("board-1", "card-2")).contains(second);
        assertThat(store.checkpoint()).isEqualTo(checkpoint);
    }

    @Test
    void 從資料庫載入的聚合_行為與存入前相同() {
        Board loaded = new EventSourcedBoardRepository(events).load(new BoardId("board-1")).orElseThrow();

        assertThat(loaded.persistedVersion()).isEqualTo(8);
        // 「進行中」上限 2、已有 1 張：再放 1 張可以，放第 3 張會被拒絕
        loaded.moveCard(new CardId("card-2"), new ColumnId("col-doing"), new Position(0), AT);
        assertThat(loaded.uncommittedEvents()).hasSize(1);
    }
}
