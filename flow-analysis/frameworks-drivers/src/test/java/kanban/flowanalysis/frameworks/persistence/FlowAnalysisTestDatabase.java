package kanban.flowanalysis.frameworks.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 流程分析測試用的 PostgreSQL：整個測試執行期間共用一個容器。
 *
 * <p>需要乾淨的資料庫時，把 flow_analysis schema 整個砍掉再重跑流程分析的資料庫結構；
 * 事件表的唯讀觸發程序永遠保持啟用，測試不會停用或繞過它。
 */
public final class FlowAnalysisTestDatabase {

    private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer("postgres:17-alpine");

    static {
        CONTAINER.start();
    }

    private FlowAnalysisTestDatabase() {
    }

    public static DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(CONTAINER.getJdbcUrl());
        dataSource.setUser(CONTAINER.getUsername());
        dataSource.setPassword(CONTAINER.getPassword());
        return dataSource;
    }

    /** 砍掉 flow_analysis schema 再照正式的資料庫結構檔重建。 */
    public static DataSource freshDatabase() {
        DataSource dataSource = dataSource();
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS flow_analysis CASCADE");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        FlowAnalysisSchemaMigration.migrate(dataSource);
        return dataSource;
    }
}
