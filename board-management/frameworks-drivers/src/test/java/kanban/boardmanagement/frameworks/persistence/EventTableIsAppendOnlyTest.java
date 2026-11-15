package kanban.boardmanagement.frameworks.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import kanban.boardmanagement.adapters.eventstore.BoardEventJson;
import kanban.boardmanagement.adapters.eventstore.PostgresEventStore;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** ES-2：事件紀錄只能新增。資料庫本身就拒絕修改與刪除，不只靠程式不去呼叫。 */
class EventTableIsAppendOnlyTest {

    private static DataSource dataSource;

    @BeforeAll
    static void oneEventExists() {
        dataSource = PostgresTestDatabase.freshDatabase();
        new PostgresEventStore(dataSource, new BoardEventJson()).append("board-x", 0, List.of(
                new BoardCreated(new BoardId("x"), new BoardName("看板"), Instant.parse("2026-11-14T01:00:00Z"))));
    }

    private static void assertRejected(String sql) {
        assertThatThrownBy(() -> {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }).isInstanceOf(SQLException.class).hasMessageContaining("append-only");
    }

    private static long eventCount() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT count(*) FROM board_events")) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Test
    void 修改事件_被資料庫拒絕() throws SQLException {
        assertRejected("UPDATE board_events SET event_type = 'Tampered'");
        assertRejected("UPDATE board_events SET payload = '{}'::jsonb WHERE stream_id = 'board-x'");
        assertThat(eventCount()).isEqualTo(1);
    }

    @Test
    void 刪除事件_被資料庫拒絕() throws SQLException {
        assertRejected("DELETE FROM board_events");
        assertThat(eventCount()).isEqualTo(1);
    }

    @Test
    void 清空事件表_被資料庫拒絕() throws SQLException {
        assertRejected("TRUNCATE board_events");
        assertThat(eventCount()).isEqualTo(1);
    }
}
