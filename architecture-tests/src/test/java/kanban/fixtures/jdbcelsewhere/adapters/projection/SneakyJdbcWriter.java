package kanban.fixtures.jdbcelsewhere.adapters.projection;

import javax.sql.DataSource;

/** 違規樣本（ES-1）：事件庫與讀取模型以外的類別直接碰資料庫。 */
public final class SneakyJdbcWriter {
    DataSource dataSource;
}
