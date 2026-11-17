package kanban.main;

import java.time.ZoneId;
import javax.sql.DataSource;
import kanban.boardmanagement.adapters.eventstore.BoardEventJson;
import kanban.boardmanagement.adapters.eventstore.EventSourcedBoardRepository;
import kanban.boardmanagement.adapters.eventstore.InMemoryEventStore;
import kanban.boardmanagement.adapters.eventstore.NotifyingEventStore;
import kanban.boardmanagement.adapters.eventstore.PostgresEventStore;
import kanban.boardmanagement.adapters.projection.BoardActivityLogStore;
import kanban.boardmanagement.adapters.projection.BoardActivityProjection;
import kanban.boardmanagement.adapters.projection.BoardProjection;
import kanban.boardmanagement.adapters.projection.InMemoryBoardActivityLog;
import kanban.boardmanagement.adapters.projection.InMemoryReadModelStore;
import kanban.boardmanagement.adapters.projection.PostgresBoardActivityLog;
import kanban.boardmanagement.adapters.projection.PostgresReadModelStore;
import kanban.boardmanagement.adapters.projection.ReadModelStore;
import kanban.boardmanagement.adapters.system.SystemTimeSource;
import kanban.boardmanagement.adapters.system.UuidIdentityGenerator;
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
 * 儲存用哪一種由設定 kanban.storage 決定：postgres（預設）或 memory。資料庫連線在 DatabaseWiring。
 */
@Configuration
class BoardManagementWiring {

    // ---------------------------------------------------------------- 儲存：PostgreSQL

    @Configuration
    @ConditionalOnProperty(name = "kanban.storage", havingValue = "postgres", matchIfMissing = true)
    static class PostgresStorage {

        @Bean
        PostgresEventStore eventStore(DataSource dataSource) {
            return new PostgresEventStore(dataSource, new BoardEventJson());
        }

        @Bean
        ReadModelStore readModelStore(DataSource dataSource) {
            return new PostgresReadModelStore(dataSource);
        }

        @Bean
        BoardActivityLogStore boardActivityLogStore(DataSource dataSource) {
            return new PostgresBoardActivityLog(dataSource);
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

        @Bean
        BoardActivityLogStore boardActivityLogStore() {
            return new InMemoryBoardActivityLog();
        }
    }

    // ---------------------------------------------------------------- 兩種儲存共用的接線

    /** Projection：讀取模型唯一的寫入途徑。 */
    @Bean
    BoardProjection boardProjection(EventFeed feed, ReadModelStore readModel) {
        return new BoardProjection(feed, readModel);
    }

    /** 看板動態的 Projection：看板動態紀錄唯一的寫入途徑（第二階段設計第 4.3 節）。 */
    @Bean
    BoardActivityProjection boardActivityProjection(EventFeed feed, BoardActivityLogStore activityLog) {
        return new BoardActivityProjection(feed, activityLog);
    }

    /**
     * 指令端的看板存取：事件寫入成功後，同一個請求內先叫看板現況的 Projection、再叫看板動態的 Projection 補到最新
     * （第一階段設計 7.5、第二階段設計 7.6 情況 1）。兩個通知各自失敗都不影響寫入結果，也不影響另一個。
     */
    @Bean
    BoardRepository boardRepository(EventStore eventStore, BoardProjection projection,
                                    BoardActivityProjection activityProjection) {
        return new EventSourcedBoardRepository(new NotifyingEventStore(
                new NotifyingEventStore(eventStore, projection::catchUp), activityProjection::catchUp));
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

    /** 啟動時把看板動態紀錄補到最新：第一次升級到第二階段時，會從第一筆事件補齊全部歷史（ES-8）。寫法同 startupCatchUp。 */
    @Bean
    ApplicationRunner boardActivityStartupCatchUp(BoardActivityProjection activityProjection) {
        return arguments -> {
            try {
                activityProjection.catchUp();
            } catch (RuntimeException failure) {
                System.getLogger(BoardManagementWiring.class.getName()).log(System.Logger.Level.ERROR,
                        "啟動時看板動態紀錄沒有補到最新；流程分析會停在舊資料，請依錯誤內容處理後重新啟動", failure);
            }
        };
    }
}
