package kanban.boardmanagement.frameworks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** ES-1：寫入端只有事件紀錄，沒有另存「聚合現況」的資料表。 */
class WriteSideSchemaTest {

    private static DataSource dataSource;

    @BeforeAll
    static void freshDatabase() {
        dataSource = PostgresTestDatabase.freshDatabase();
    }

    private static List<String> query(String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }

    @Test
    void 寫入端只有事件表() throws SQLException {
        // board_events 是寫入端唯一的表；其餘四張是可隨時清掉重建的讀取模型，flyway_schema_history 是資料庫版本紀錄
        assertThat(query("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"))
                .containsExactlyInAnyOrder("board_events", "board_view", "card_history_view",
                        "projection_checkpoint", "flyway_schema_history",
                        // 看板動態紀錄：讀取模型，可以清掉重建（第二階段設計第 0 節 4d）
                        "board_activity_log");
    }

    @Test
    void 事件表的欄位就是事件本身_沒有現況欄位() throws SQLException {
        assertThat(query("SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'board_events' ORDER BY ordinal_position"))
                .containsExactly("global_position", "stream_id", "stream_version", "event_type", "schema_version",
                        "payload", "occurred_at");
    }

    @Test
    void 同一條串流的版本號不可重複() throws SQLException {
        // 樂觀鎖在資料庫層也有保證（ES-6）
        assertThat(query("SELECT indexdef FROM pg_indexes WHERE tablename = 'board_events'"))
                .anyMatch(index -> index.contains("UNIQUE") && index.contains("stream_id")
                        && index.contains("stream_version"));
    }
}
