package kanban.boardmanagement.adapters.projection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import tools.jackson.databind.json.JsonMapper;

/**
 * PostgreSQL 版看板動態紀錄。每筆動態存成一筆 JSON 文件，以全域位置為主鍵。只用 JDK 的 java.sql（CA-7）。
 * 處理位置和看板現況那份讀取模型共用 projection_checkpoint 表，但名稱不同、各自前進。
 */
public final class PostgresBoardActivityLog implements BoardActivityLogStore {

    /** 同一時間只有一個看板動態 Projection 在更新（任意固定值，與事件庫、看板現況的鎖不同）。 */
    private static final long PROJECTION_LOCK = 7_238_116L;
    private static final String CHECKPOINT_NAME = "board-activity-log";

    private final DataSource dataSource;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ThreadLocal<Connection> transaction = new ThreadLocal<>();

    public PostgresBoardActivityLog(DataSource dataSource) {
        this.dataSource = dataSource;
    }

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
            throw new IllegalStateException("更新看板動態紀錄失敗", e);
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
            throw new IllegalStateException("讀取看板動態紀錄失敗", e);
        }
    }

    private void write(Query<Void> change) {
        Connection bound = transaction.get();
        if (bound == null) {
            throw new IllegalStateException("看板動態紀錄的寫入只能在 inExclusiveTransaction 之內");
        }
        try {
            change.run(bound);
        } catch (SQLException e) {
            throw new IllegalStateException("寫入看板動態紀錄失敗", e);
        }
    }

    @Override
    public void append(BoardActivity activity) {
        write(connection -> {
            String sql = "INSERT INTO board_activity_log (position, doc) VALUES (?, ?::jsonb) "
                    + "ON CONFLICT (position) DO UPDATE SET doc = EXCLUDED.doc";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, activity.position());
                statement.setString(2, mapper.writeValueAsString(activity));
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void advanceCheckpoint(long position) {
        write(connection -> {
            String sql = "INSERT INTO projection_checkpoint (name, position) VALUES (?, ?) "
                    + "ON CONFLICT (name) DO UPDATE SET position = EXCLUDED.position";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, CHECKPOINT_NAME);
                statement.setLong(2, position);
                statement.executeUpdate();
            }
            return null;
        });
    }

    /** 只清自己：看板動態紀錄與自己的處理位置；看板現況那份讀取模型不受影響。 */
    @Override
    public void reset() {
        write(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("TRUNCATE board_activity_log");
            }
            try (PreparedStatement statement =
                         connection.prepareStatement("DELETE FROM projection_checkpoint WHERE name = ?")) {
                statement.setString(1, CHECKPOINT_NAME);
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public long checkpoint() {
        return read(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT position FROM projection_checkpoint WHERE name = ?")) {
                statement.setString(1, CHECKPOINT_NAME);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? rows.getLong(1) : 0L;
                }
            }
        });
    }

    @Override
    public List<BoardActivity> activitiesAfter(long position, int maxCount) {
        return read(connection -> {
            String sql = "SELECT doc::text FROM board_activity_log WHERE position > ? ORDER BY position LIMIT ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, position);
                statement.setInt(2, maxCount);
                try (ResultSet rows = statement.executeQuery()) {
                    List<BoardActivity> activities = new ArrayList<>();
                    while (rows.next()) {
                        activities.add(mapper.readValue(rows.getString(1), BoardActivity.class));
                    }
                    return activities;
                }
            }
        });
    }
}
