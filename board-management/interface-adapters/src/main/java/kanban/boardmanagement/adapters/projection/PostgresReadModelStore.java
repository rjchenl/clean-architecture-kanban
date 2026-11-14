package kanban.boardmanagement.adapters.projection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import kanban.boardmanagement.usecases.queries.listboards.BoardSummary;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;
import tools.jackson.databind.json.JsonMapper;

/**
 * PostgreSQL 版讀取模型。每份讀取模型存成一筆 JSON 文件。只用 JDK 的 java.sql；連線來源由 Main 注入（CA-7）。
 */
public final class PostgresReadModelStore implements ReadModelStore {

    /** 同一時間只有一個 Projection 在更新讀取模型（任意固定值，與事件庫的鎖不同）。 */
    private static final long PROJECTION_LOCK = 7_238_115L;
    private static final String CHECKPOINT_NAME = "board-management";

    private final DataSource dataSource;
    private final JsonMapper mapper = JsonMapper.builder().build();
    /** 目前這條執行緒在 inExclusiveTransaction 裡用的連線；不在交易裡時為空。 */
    private final ThreadLocal<Connection> transaction = new ThreadLocal<>();

    public PostgresReadModelStore(DataSource dataSource) {
        this.dataSource = dataSource;
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
            throw new IllegalStateException("更新讀取模型失敗", e);
        }
    }

    private interface Query<T> {
        T run(Connection connection) throws SQLException;
    }

    /** 交易中用交易的連線（讀得到自己剛寫的資料）；否則另開一條。 */
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
            throw new IllegalStateException("讀取讀取模型失敗", e);
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
            throw new IllegalStateException("寫入讀取模型失敗", e);
        }
    }

    // ------------------------------------------------------------------ 寫入（只有 Projection 呼叫）

    @Override
    public void saveBoard(BoardSnapshot board) {
        write(connection -> {
            String sql = "INSERT INTO board_view (board_id, doc) VALUES (?, ?::jsonb) "
                    + "ON CONFLICT (board_id) DO UPDATE SET doc = EXCLUDED.doc";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, board.boardId());
                statement.setString(2, mapper.writeValueAsString(board));
                statement.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public void saveCardHistory(String boardId, CardHistory history) {
        write(connection -> {
            String sql = "INSERT INTO card_history_view (board_id, card_id, doc) VALUES (?, ?, ?::jsonb) "
                    + "ON CONFLICT (board_id, card_id) DO UPDATE SET doc = EXCLUDED.doc";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, boardId);
                statement.setString(2, history.cardId());
                statement.setString(3, mapper.writeValueAsString(history));
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

    @Override
    public void reset() {
        write(connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("TRUNCATE board_view, card_history_view, projection_checkpoint RESTART IDENTITY");
            }
            return null;
        });
    }

    // ------------------------------------------------------------------ 讀取

    @Override
    public long checkpoint() {
        return read(connection -> {
            String sql = "SELECT position FROM projection_checkpoint WHERE name = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, CHECKPOINT_NAME);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? rows.getLong(1) : 0L;
                }
            }
        });
    }

    @Override
    public Optional<BoardSnapshot> boardById(String boardId) {
        return read(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT doc::text FROM board_view WHERE board_id = ?")) {
                statement.setString(1, boardId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next()
                            ? Optional.of(mapper.readValue(rows.getString(1), BoardSnapshot.class))
                            : Optional.<BoardSnapshot>empty();
                }
            }
        });
    }

    @Override
    public List<BoardSummary> allBoards() {
        return read(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT doc::text FROM board_view ORDER BY created_order")) {
                List<BoardSummary> boards = new ArrayList<>();
                while (rows.next()) {
                    BoardSnapshot board = mapper.readValue(rows.getString(1), BoardSnapshot.class);
                    boards.add(new BoardSummary(board.boardId(), board.name(), board.archived()));
                }
                return boards;
            }
        });
    }

    @Override
    public Optional<CardHistory> cardHistory(String boardId, String cardId) {
        return read(connection -> {
            String sql = "SELECT doc::text FROM card_history_view WHERE board_id = ? AND card_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, boardId);
                statement.setString(2, cardId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next()
                            ? Optional.of(mapper.readValue(rows.getString(1), CardHistory.class))
                            : Optional.<CardHistory>empty();
                }
            }
        });
    }
}
