package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import kanban.boardmanagement.adapters.eventstore.BoardEventJson;
import kanban.boardmanagement.adapters.eventstore.PostgresEventStore;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.entities.board.ColumnAdded;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.frameworks.persistence.SchemaMigration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** 啟動時事件紀錄有資料、讀取模型卻是空的（例如被清掉過）→ 啟動後自動補齊（ES-8）。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "kanban.test.context=startup-catch-up")
class StartupCatchUpIntegrationTest extends HttpTestSupport {

    private static final String BOARD = "board-written-before-startup";

    /** 這個方法在應用程式啟動「之前」執行：先寫入事件，再把讀取模型清空。 */
    @DynamicPropertySource
    static void eventsExistButReadModelIsEmpty(DynamicPropertyRegistry registry) throws Exception {
        PostgresContainer.register(registry);
        DataSource dataSource = PostgresContainer.dataSource();
        SchemaMigration.migrate(dataSource);
        Instant at = Instant.parse("2026-11-14T01:00:00Z");
        new PostgresEventStore(dataSource, new BoardEventJson()).append("board-" + BOARD, 0, List.of(
                new BoardCreated(new BoardId(BOARD), new BoardName("啟動前就存在的看板"), at),
                new ColumnAdded(new BoardId(BOARD), new ColumnId("col-todo"), new ColumnName("待辦"), at)));
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE board_view, card_history_view, projection_checkpoint RESTART IDENTITY");
        }
    }

    @Test
    void 啟動時讀取模型是空的_自動補齊() {
        Reply view = get("/api/boards/" + BOARD);

        assertThat(view.status()).isEqualTo(200);
        assertThat(view.text("name")).isEqualTo("啟動前就存在的看板");
        assertThat(view.body().get("columns").get(0).get("name").asString()).isEqualTo("待辦");
    }
}
