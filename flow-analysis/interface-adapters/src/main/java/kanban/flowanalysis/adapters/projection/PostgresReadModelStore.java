package kanban.flowanalysis.adapters.projection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.Delivery;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetrics;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.WeeklyCompletion;
import tools.jackson.databind.json.JsonMapper;

/**
 * PostgreSQL 版流程分析讀取模型，表在 flow_analysis schema。只用 JDK 的 java.sql（CA-7）；連線來源由 Main 注入。
 * 兩個情境共用同一個資料庫連線，連線預設的 schema 是 public，所以表名一律寫 flow_analysis 前綴。
 * 統計方式 S1 由 {@link WeekCalendar} 決定、存成 week_starts_at；S2 的計數、平均（四捨五入到整數秒）、最長用 SQL 算。
 */
public final class PostgresReadModelStore implements ReadModelStore {

    /** 同一時間只有一個流程分析 Projection 在更新讀取模型（任意固定值，和其他鎖不同）。 */
    private static final long PROJECTION_LOCK = 7_238_215L;
    private static final String CHECKPOINT_NAME = "flow-analysis";
    private static final int RECENT_WEEKS = 8;

    private final DataSource dataSource;
    private final WeekCalendar calendar;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ThreadLocal<Connection> transaction = new ThreadLocal<>();

    public PostgresReadModelStore(DataSource dataSource, WeekCalendar calendar) {
        this.dataSource = dataSource;
        this.calendar = calendar;
    }

    // ------------------------------------------------------------------ 交易

    @Override
    public void inExclusiveTransaction(Runnable work) {
        if (transaction.get() != null) {
            work.run();
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            transaction.set(connection);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("SELECT pg_advisory_xact_lock(" + PROJECTION_LOCK + ")");
                }
                work.run();
                connection.commit();
            } catch (SQLException | RuntimeException | Error failure) {
                connection.rollback();
                throw failure;
            } finally {
                transaction.remove();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("更新流程分析讀取模型失敗", e);
        }
    }

    private interface Query<T> {
        T run(Connection connection) throws SQLException;
    }

    private <T> T read(Query<T> query) {
        Connection bound = transaction.get();
        try {
            if (bound != null) {
                return query.run(bound);
            }
            try (Connection connection = dataSource.getConnection()) {
                return query.run(connection);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("讀取流程分析讀取模型失敗", e);
        }
    }

    private void write(Query<Void> change) {
        Connection bound = transaction.get();
        if (bound == null) {
            throw new IllegalStateException("讀取模型的寫入只能在 inExclusiveTransaction 之內");
        }
        try {
            change.run(bound);
        } catch (SQLException e) {
            throw new IllegalStateException("寫入流程分析讀取模型失敗", e);
        }
    }

    // ------------------------------------------------------------------ 寫入（只有 Projection 呼叫）

    @Override
    public void saveBoardFlow(FlowBoardDocument document) {
        write(connection -> {
            String sql = "INSERT INTO flow_analysis.board_flow_view (board_flow_id, doc) VALUES (?, ?::jsonb) "
                    + "ON CONFLICT (board_flow_id) DO UPDATE SET doc = EXCLUDED.doc";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, document.boardFlowId());
                statement.setString(2, mapper.writeValueAsString(document));
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void saveCompletion(String boardFlowId, String workItemId, Instant completedAt, long leadTimeSeconds) {
        write(connection -> {
            String sql = "INSERT INTO flow_analysis.completed_work_item_view "
                    + "(board_flow_id, work_item_id, completed_at, week_starts_at, lead_time_seconds) VALUES (?, ?, ?, ?, ?) "
                    + "ON CONFLICT (board_flow_id, work_item_id) DO UPDATE SET completed_at = EXCLUDED.completed_at, "
                    + "week_starts_at = EXCLUDED.week_starts_at, lead_time_seconds = EXCLUDED.lead_time_seconds";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, boardFlowId);
                statement.setString(2, workItemId);
                statement.setObject(3, utc(completedAt));
                statement.setObject(4, utc(calendar.weekStartOf(completedAt)));
                statement.setLong(5, leadTimeSeconds);
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void removeCompletion(String boardFlowId, String workItemId) {
        write(connection -> {
            String sql = "DELETE FROM flow_analysis.completed_work_item_view WHERE board_flow_id = ? AND work_item_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, boardFlowId);
                statement.setString(2, workItemId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void advanceCheckpoint(long position) {
        write(connection -> {
            String sql = "INSERT INTO flow_analysis.projection_checkpoint (name, position) VALUES (?, ?) "
                    + "ON CONFLICT (name) DO UPDATE SET position = EXCLUDED.position";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, CHECKPOINT_NAME);
                statement.setLong(2, position);
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void reset() {
        write(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("TRUNCATE flow_analysis.board_flow_view, flow_analysis.completed_work_item_view, "
                        + "flow_analysis.projection_checkpoint RESTART IDENTITY");
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ 讀取

    @Override
    public long checkpoint() {
        return read(connection -> {
            String sql = "SELECT position FROM flow_analysis.projection_checkpoint WHERE name = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, CHECKPOINT_NAME);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? rows.getLong(1) : 0L;
                }
            }
        });
    }

    @Override
    public Optional<FlowBoardDocument> boardFlowDocument(String boardFlowId) {
        return read(connection -> documentOf(connection, boardFlowId));
    }

    @Override
    public List<String> allBoardFlowIds() {
        return read(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT board_flow_id FROM flow_analysis.board_flow_view ORDER BY created_order")) {
                List<String> ids = new ArrayList<>();
                while (rows.next()) {
                    ids.add(rows.getString(1));
                }
                return ids;
            }
        });
    }

    @Override
    public Optional<FlowMetrics> flowMetrics(String boardFlowId, Instant asOf) {
        return read(connection -> {
            Optional<FlowBoardDocument> doc = documentOf(connection, boardFlowId);
            if (doc.isEmpty()) {
                return Optional.<FlowMetrics>empty();
            }
            List<Instant> weekStarts = calendar.recentWeekStarts(asOf, RECENT_WEEKS);
            Instant from = weekStarts.getFirst();
            Instant to = weekStarts.getLast();
            return Optional.of(FlowMetricsAssembler.assemble(doc.get(),
                    weeklyCompletions(connection, boardFlowId, weekStarts, from, to),
                    delivery(connection, boardFlowId, from, to)));
        });
    }

    private Optional<FlowBoardDocument> documentOf(Connection connection, String boardFlowId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT doc::text FROM flow_analysis.board_flow_view WHERE board_flow_id = ?")) {
            statement.setString(1, boardFlowId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next()
                        ? Optional.of(mapper.readValue(rows.getString(1), FlowBoardDocument.class))
                        : Optional.empty();
            }
        }
    }

    private static List<WeeklyCompletion> weeklyCompletions(Connection connection, String boardFlowId,
            List<Instant> weekStarts, Instant from, Instant to) throws SQLException {
        String sql = "SELECT week_starts_at, COUNT(*) FROM flow_analysis.completed_work_item_view "
                + "WHERE board_flow_id = ? AND week_starts_at BETWEEN ? AND ? GROUP BY week_starts_at";
        Map<Instant, Integer> counts = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, boardFlowId);
            statement.setObject(2, utc(from));
            statement.setObject(3, utc(to));
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    counts.put(rows.getObject(1, OffsetDateTime.class).toInstant(), rows.getInt(2));
                }
            }
        }
        return weekStarts.stream().map(start -> new WeeklyCompletion(start, counts.getOrDefault(start, 0))).toList();
    }

    private static Delivery delivery(Connection connection, String boardFlowId, Instant from, Instant to)
            throws SQLException {
        String sql = "SELECT COUNT(*), ROUND(AVG(lead_time_seconds))::bigint, MAX(lead_time_seconds) "
                + "FROM flow_analysis.completed_work_item_view WHERE board_flow_id = ? AND week_starts_at BETWEEN ? AND ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, boardFlowId);
            statement.setObject(2, utc(from));
            statement.setObject(3, utc(to));
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                int count = rows.getInt(1);
                if (count == 0) {
                    return new Delivery(0, null, null);
                }
                return new Delivery(count, rows.getLong(2), rows.getLong(3));
            }
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
