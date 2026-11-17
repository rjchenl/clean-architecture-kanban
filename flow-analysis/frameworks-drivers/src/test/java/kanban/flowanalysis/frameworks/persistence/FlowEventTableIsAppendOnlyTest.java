package kanban.flowanalysis.frameworks.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import kanban.flowanalysis.adapters.eventstore.BoardFlowEventJson;
import kanban.flowanalysis.adapters.eventstore.PostgresEventStore;
import kanban.flowanalysis.entities.boardflow.ActivityPosition;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** ES-2：流程分析的事件紀錄只能新增；修改、刪除、清空一律由資料庫拒絕。 */
class FlowEventTableIsAppendOnlyTest {

    private static DataSource dataSource;

    @BeforeAll
    static void oneEventExists() {
        dataSource = FlowAnalysisTestDatabase.freshDatabase();
        new PostgresEventStore(dataSource, new BoardFlowEventJson()).append("board-flow-1", 0, List.of(
                new BoardFlowCreated(new BoardFlowId("board-1"), new ActivityPosition(1), new FlowBoardName("產品開發"),
                        Instant.parse("2026-11-16T01:00:00Z"))));
    }

    private static void assertRejected(String sql) {
        assertThatThrownBy(() -> {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }).isInstanceOf(SQLException.class).hasMessageContaining("append-only");
    }

    @Test
    void 修改事件_被資料庫拒絕() {
        assertRejected("UPDATE flow_analysis.board_flow_events SET event_type = 'X'");
    }

    @Test
    void 刪除事件_被資料庫拒絕() {
        assertRejected("DELETE FROM flow_analysis.board_flow_events");
    }

    @Test
    void 清空事件表_被資料庫拒絕() {
        assertRejected("TRUNCATE flow_analysis.board_flow_events");
    }
}
