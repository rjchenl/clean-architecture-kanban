package kanban.fixtures.jdbcsamename.adapters.system;

import java.sql.Connection;

/** 違規樣本（ES-1，2026-11-17 獨立審查 M-6）：取了允許清單上的簡名，就在別的套件碰資料庫。 */
public final class PostgresReadModelStore {
    Connection connection;
}
