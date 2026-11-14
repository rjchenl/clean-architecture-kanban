package kanban.boardmanagement.frameworks.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/** 把資料庫結構更新到最新版（db/migration 底下的 SQL）。由 Main 在啟動時呼叫。 */
public final class SchemaMigration {

    private SchemaMigration() {
    }

    public static void migrate(DataSource dataSource) {
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }
}
