package kanban.flowanalysis.frameworks.persistence;

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

/** ES-1：流程分析的寫入端只有事件紀錄，沒有另存「看板流程現況」的資料表。 */
class FlowAnalysisSchemaTest {

    private static DataSource dataSource;

    @BeforeAll
    static void migrate() {
        dataSource = FlowAnalysisTestDatabase.freshDatabase();
    }

    @Test
    void 寫入端只有事件表() throws SQLException {
        // board_flow_events 是寫入端唯一的表；board_flow_view、completed_work_item_view、projection_checkpoint 是可以清掉重建的讀取模型；
        // board_activity_position 是防腐層的同步進度；flyway_schema_history 是資料庫版本紀錄
        assertThat(query("SELECT table_name FROM information_schema.tables WHERE table_schema = 'flow_analysis'"))
                .containsExactlyInAnyOrder("board_flow_events", "board_flow_view", "completed_work_item_view",
                        "projection_checkpoint", "board_activity_position", "flyway_schema_history");
    }

    @Test
    void 事件表的欄位就是事件本身_沒有現況欄位() throws SQLException {
        assertThat(query("SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = 'flow_analysis' AND table_name = 'board_flow_events' ORDER BY ordinal_position"))
                .containsExactly("global_position", "stream_id", "stream_version", "event_type", "schema_version",
                        "payload", "occurred_at");
    }

    @Test
    void 同一條串流的版本號不可重複() throws SQLException {
        assertThat(query("SELECT indexdef FROM pg_indexes WHERE schemaname = 'flow_analysis' AND tablename = 'board_flow_events'"))
                .anyMatch(index -> index.contains("UNIQUE") && index.contains("stream_id") && index.contains("stream_version"));
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
}
