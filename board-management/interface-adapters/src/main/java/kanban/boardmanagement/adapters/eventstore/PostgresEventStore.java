package kanban.boardmanagement.adapters.eventstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.usecases.commands.gateway.ConcurrentUpdateException;
import kanban.boardmanagement.usecases.commands.gateway.EventStore;
import kanban.boardmanagement.usecases.eventfeed.EventFeed;
import kanban.boardmanagement.usecases.eventfeed.RecordedEvent;

/**
 * PostgreSQL 版事件紀錄。只用 JDK 的 java.sql；連線來源由 Main 注入（CA-7）。
 *
 * <p>寫入時先取一把交易層級的鎖，讓全域位置的順序等於實際提交的順序，
 * Projection 依位置往後讀才不會漏掉「位置較小卻較晚提交」的事件。
 */
public final class PostgresEventStore implements EventStore, EventFeed {

    /** 所有寫入共用的鎖編號（任意固定值）。 */
    private static final long APPEND_LOCK = 7_238_114L;
    private static final String UNIQUE_VIOLATION = "23505";

    private final DataSource dataSource;
    private final BoardEventJson json;

    public PostgresEventStore(DataSource dataSource, BoardEventJson json) {
        this.dataSource = dataSource;
        this.json = json;
    }

    @Override
    public List<BoardEvent> readStream(String streamId) {
        String sql = "SELECT event_type, schema_version, payload::text FROM board_events "
                + "WHERE stream_id = ? ORDER BY stream_version";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, streamId);
            try (ResultSet rows = statement.executeQuery()) {
                List<BoardEvent> events = new ArrayList<>();
                while (rows.next()) {
                    events.add(eventOf(rows));
                }
                return events;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("讀取事件紀錄失敗", e);
        }
    }

    @Override
    public void append(String streamId, long expectedVersion, List<BoardEvent> events) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockAppends(connection);
                if (currentVersion(connection, streamId) != expectedVersion) {
                    throw new ConcurrentUpdateException(streamId, expectedVersion);
                }
                insert(connection, streamId, expectedVersion, events);
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        } catch (SQLException e) {
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new ConcurrentUpdateException(streamId, expectedVersion);
            }
            throw new IllegalStateException("寫入事件紀錄失敗", e);
        }
    }

    @Override
    public List<RecordedEvent> readAfter(long position, int maxCount) {
        String sql = "SELECT event_type, schema_version, payload::text, global_position FROM board_events "
                + "WHERE global_position > ? ORDER BY global_position LIMIT ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, position);
            statement.setInt(2, maxCount);
            try (ResultSet rows = statement.executeQuery()) {
                List<RecordedEvent> events = new ArrayList<>();
                while (rows.next()) {
                    long eventPosition = rows.getLong(4);
                    try {
                        events.add(new RecordedEvent(eventPosition, eventOf(rows)));
                    } catch (UnknownEventTypeException unreadable) {
                        // 讀不懂的那一筆不可以連累它前面的正常事件：先把前面的交出去。
                        // 下一次從它前一筆之後再讀，它就是第一筆，屆時明確報錯（不會被默默略過）。
                        if (events.isEmpty()) {
                            throw unreadable;
                        }
                        return events;
                    }
                }
                return events;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("讀取事件紀錄失敗", e);
        }
    }

    private BoardEvent eventOf(ResultSet rows) throws SQLException {
        return json.fromStored(new StoredEvent(rows.getString(1), rows.getInt(2), rows.getString(3)));
    }

    private static void lockAppends(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT pg_advisory_xact_lock(" + APPEND_LOCK + ")");
        }
    }

    private static long currentVersion(Connection connection, String streamId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(stream_version), 0) FROM board_events WHERE stream_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, streamId);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getLong(1);
            }
        }
    }

    private void insert(Connection connection, String streamId, long expectedVersion, List<BoardEvent> events)
            throws SQLException {
        String sql = "INSERT INTO board_events (stream_id, stream_version, event_type, schema_version, payload, "
                + "occurred_at) VALUES (?, ?, ?, ?, ?::jsonb, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            long version = expectedVersion;
            for (BoardEvent event : events) {
                StoredEvent stored = json.toStored(event);
                statement.setString(1, streamId);
                statement.setLong(2, ++version);
                statement.setString(3, stored.eventType());
                statement.setInt(4, stored.schemaVersion());
                statement.setString(5, stored.payloadJson());
                statement.setObject(6, OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
