package kanban.main;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import kanban.boardmanagement.frameworks.persistence.SchemaMigration;
import kanban.flowanalysis.frameworks.persistence.FlowAnalysisSchemaMigration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 兩個業務情境共用的資料庫連線（CA-8）：同一個 PostgreSQL，各用自己的 schema 與自己的資料庫版本紀錄。
 * 建立連線後依序把兩個情境的資料庫結構更新到最新。只在 kanban.storage=postgres（預設）時建立。
 */
@Configuration
@ConditionalOnProperty(name = "kanban.storage", havingValue = "postgres", matchIfMissing = true)
class DatabaseWiring {

    @Bean(destroyMethod = "close")
    HikariDataSource dataSource(@Value("${kanban.datasource.url}") String url,
                                @Value("${kanban.datasource.username}") String username,
                                @Value("${kanban.datasource.password}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        HikariDataSource dataSource = new HikariDataSource(config);
        SchemaMigration.migrate(dataSource);
        FlowAnalysisSchemaMigration.migrate(dataSource);
        return dataSource;
    }
}
