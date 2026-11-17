package kanban.flowanalysis.adapters.boardactivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * PostgreSQL 版的讀取位置，存在流程分析自己的 schema。
 * 兩個情境共用同一個資料庫連線，連線預設的 schema 是 public，所以表名一律寫 flow_analysis 前綴。
 */
public final class PostgresActivityPositionStore implements ActivityPositionStore {

    /** 讀的是哪一個上游公布的動態；目前只有看板管理。 */
    private static final String UPSTREAM = "board-management";

    private final DataSource dataSource;

    public PostgresActivityPositionStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public long lastPosition() {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement query = connection.prepareStatement(
                        "SELECT position FROM flow_analysis.board_activity_position WHERE name = ?")) {
            query.setString(1, UPSTREAM);
            try (ResultSet row = query.executeQuery()) {
                return row.next() ? row.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("讀不到防腐層的讀取位置", e);
        }
    }

    @Override
    public void advanceTo(long newPosition) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long current = currentForUpdate(connection);
                if (newPosition < current) {
                    throw new IllegalArgumentException("讀取位置不能倒退：目前 " + current + "，要求 " + newPosition);
                }
                try (PreparedStatement upsert = connection.prepareStatement(
                        "INSERT INTO flow_analysis.board_activity_position (name, position) VALUES (?, ?) "
                                + "ON CONFLICT (name) DO UPDATE SET position = EXCLUDED.position")) {
                    upsert.setString(1, UPSTREAM);
                    upsert.setLong(2, newPosition);
                    upsert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("存不進防腐層的讀取位置", e);
        }
    }

    private static long currentForUpdate(Connection connection) throws SQLException {
        try (PreparedStatement lock = connection.prepareStatement(
                "SELECT position FROM flow_analysis.board_activity_position WHERE name = ? FOR UPDATE")) {
            lock.setString(1, UPSTREAM);
            try (ResultSet row = lock.executeQuery()) {
                return row.next() ? row.getLong(1) : 0;
            }
        }
    }
}
