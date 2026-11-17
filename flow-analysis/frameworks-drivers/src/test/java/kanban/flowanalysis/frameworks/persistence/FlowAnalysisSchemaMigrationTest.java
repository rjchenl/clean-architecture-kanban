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

/** 流程分析的資料庫結構放在自己的 schema，有自己的資料庫版本紀錄，不碰看板管理的 public（設計第 8 節）。 */
class FlowAnalysisSchemaMigrationTest {

    private static DataSource dataSource;

    @BeforeAll
    static void migrate() {
        dataSource = FlowAnalysisTestDatabase.freshDatabase();
    }

    @Test
    void 建立自己的schema與版本紀錄() throws SQLException {
        // 只斷言包含；「恰好哪幾張表」由寫入端結構檢查（ES-1）斷言
        assertThat(query("SELECT table_name FROM information_schema.tables WHERE table_schema = 'flow_analysis'"))
                .contains("board_activity_position", "flyway_schema_history");
    }

    @Test
    void 不碰public() throws SQLException {
        assertThat(query("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"))
                .doesNotContain("flyway_schema_history", "board_activity_position");
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
