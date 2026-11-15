package kanban.main;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 整合測試共用的 PostgreSQL 容器（整個測試執行期間只啟動一次）。 */
final class PostgresContainer {

    private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer("postgres:17-alpine");

    static {
        CONTAINER.start();
    }

    private PostgresContainer() {
    }

    /** 讓應用程式連到這個容器。 */
    static void register(DynamicPropertyRegistry registry) {
        registry.add("kanban.datasource.url", CONTAINER::getJdbcUrl);
        registry.add("kanban.datasource.username", CONTAINER::getUsername);
        registry.add("kanban.datasource.password", CONTAINER::getPassword);
    }

    static DataSource dataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(CONTAINER.getJdbcUrl());
        dataSource.setUser(CONTAINER.getUsername());
        dataSource.setPassword(CONTAINER.getPassword());
        return dataSource;
    }
}
