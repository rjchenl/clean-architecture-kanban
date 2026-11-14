package kanban.main;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.ZoneId;
import javax.sql.DataSource;
import kanban.boardmanagement.adapters.eventstore.BoardEventJson;
import kanban.boardmanagement.adapters.eventstore.EventSourcedBoardRepository;
import kanban.boardmanagement.adapters.eventstore.InMemoryEventStore;
import kanban.boardmanagement.adapters.eventstore.NotifyingEventStore;
import kanban.boardmanagement.adapters.eventstore.PostgresEventStore;
import kanban.boardmanagement.adapters.projection.BoardProjection;
import kanban.boardmanagement.adapters.projection.InMemoryReadModelStore;
import kanban.boardmanagement.adapters.projection.PostgresReadModelStore;
import kanban.boardmanagement.adapters.projection.ReadModelStore;
import kanban.boardmanagement.adapters.system.SystemTimeSource;
import kanban.boardmanagement.adapters.system.UuidIdentityGenerator;
import kanban.boardmanagement.frameworks.persistence.SchemaMigration;
import kanban.boardmanagement.frameworks.web.BoardManagementRequests;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.EventStore;
import kanban.boardmanagement.usecases.eventfeed.EventFeed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 「看板管理」的組裝（CA-8）：建立所有具體實作並接起來。
 * 儲存用哪一種由設定 kanban.storage 決定：postgres（預設）或 memory。
 */
@Configuration
class BoardManagementWiring {

    // ---------------------------------------------------------------- 儲存：PostgreSQL

    @Configuration
    @ConditionalOnProperty(name = "kanban.storage", havingValue = "postgres", matchIfMissing = true)
    static class PostgresStorage {

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
            return dataSource;
        }

        @Bean
        PostgresEventStore eventStore(DataSource dataSource) {
            return new PostgresEventStore(dataSource, new BoardEventJson());
        }

        @Bean
        ReadModelStore readModelStore(DataSource dataSource) {
            return new PostgresReadModelStore(dataSource);
        }
    }

    // ---------------------------------------------------------------- 儲存：記憶體

    @Configuration
    @ConditionalOnProperty(name = "kanban.storage", havingValue = "memory")
    static class InMemoryStorage {

        @Bean
        InMemoryEventStore eventStore() {
            return new InMemoryEventStore();
        }

        @Bean
        ReadModelStore readModelStore() {
            return new InMemoryReadModelStore();
        }
    }

    // ---------------------------------------------------------------- 兩種儲存共用的接線

    /** Projection：讀取模型唯一的寫入途徑。 */
    @Bean
    BoardProjection boardProjection(EventFeed feed, ReadModelStore readModel) {
        return new BoardProjection(feed, readModel);
    }

    /** 指令端的看板存取：事件寫入成功後，同一個請求內叫 Projection 補到最新（設計文件 7.5）。 */
    @Bean
    BoardRepository boardRepository(EventStore eventStore, BoardProjection projection) {
        return new EventSourcedBoardRepository(new NotifyingEventStore(eventStore, projection::catchUp));
    }

    @Bean
    BoardManagementRequests boardManagementRequests(BoardRepository boards, ReadModelStore readModel,
                                                    @Value("${kanban.display-zone:Asia/Taipei}") String displayZone) {
        return new BoardManagementRequestAssembly(boards, readModel, readModel, readModel,
                new SystemTimeSource(), new UuidIdentityGenerator(), ZoneId.of(displayZone));
    }

    /**
     * 啟動時把讀取模型補到最新：上次沒補完的、或讀取模型被清空過，都在這裡補齊（ES-8）。
     * 補不上（例如事件紀錄裡有這一版不認得的事件）不讓應用程式起不來：事件紀錄完好、指令照常可用，
     * 讀取模型停在出問題的那一筆之前，錯誤寫進日誌。
     */
    @Bean
    ApplicationRunner startupCatchUp(BoardProjection projection) {
        return arguments -> {
            try {
                projection.catchUp();
            } catch (RuntimeException failure) {
                System.getLogger(BoardManagementWiring.class.getName()).log(System.Logger.Level.ERROR,
                        "啟動時讀取模型沒有補到最新；畫面會看到舊資料，請依錯誤內容處理後重新啟動", failure);
            }
        };
    }
}
