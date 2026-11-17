package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import kanban.boardmanagement.adapters.eventstore.BoardEventJson;
import kanban.boardmanagement.adapters.eventstore.PostgresEventStore;
import kanban.boardmanagement.adapters.projection.BoardActivityLogStore;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.entities.board.ColumnAdded;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.frameworks.persistence.SchemaMigration;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.flowanalysis.adapters.boardactivity.ActivityPositionStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Review Focus 4：第一階段的資料庫第一次升級到第二階段——看板動態紀錄原本是空的，
 * 啟動後自動從頭補齊；流程分析接著同步到第一階段以來的全部歷史。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "kanban.test.context=flow-analysis-first-upgrade")
class FlowAnalysisStartupIntegrationTest extends FlowAnalysisTestSupport {

    private static final String BOARD = "board-from-phase-one";

    /** 這個方法在應用程式啟動「之前」執行：寫入第一階段的看板事件，再把看板動態紀錄與它的處理位置清掉。 */
    @DynamicPropertySource
    static void phaseOneDatabase(DynamicPropertyRegistry registry) throws Exception {
        PostgresContainer.register(registry);
        DataSource dataSource = PostgresContainer.dataSource();
        SchemaMigration.migrate(dataSource);
        Instant at = Instant.parse("2026-11-10T01:00:00Z");
        new PostgresEventStore(dataSource, new BoardEventJson()).append("board-" + BOARD, 0, List.of(
                new BoardCreated(new BoardId(BOARD), new BoardName("第一階段就有的看板"), at),
                new ColumnAdded(new BoardId(BOARD), new ColumnId("col-todo"), new ColumnName("待辦"), at)));
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("TRUNCATE board_activity_log");
            statement.execute("DELETE FROM projection_checkpoint WHERE name = 'board-activity-log'");
        }
    }

    @Autowired
    BoardActivityLogStore activityLog;

    @Autowired
    ActivityPositionStore positions;

    @Test
    void 第一階段的資料庫第一次升級_看板動態補齊_流程分析同步到全部歷史() {
        List<BoardActivity> published = activityLog.activitiesAfter(0, Integer.MAX_VALUE);

        assertThat(published).as("啟動時看板動態紀錄從第一筆事件補齊").extracting(BoardActivity::boardId).contains(BOARD);

        sync();

        assertThat(positions.lastPosition()).as("流程分析同步到全部歷史").isEqualTo(published.getLast().position());
        Reply view = viewFlow(BOARD);
        assertThat(view.status()).isEqualTo(200);
        assertThat(view.text("boardName")).isEqualTo("第一階段就有的看板");
        assertThat(columns(view)).containsExactly("待辦:0");
    }
}
