package kanban.main;

import java.time.ZoneId;
import javax.sql.DataSource;
import kanban.boardmanagement.adapters.projection.BoardActivityLogStore;
import kanban.boardmanagement.usecases.queries.readboardactivities.impl.ReadBoardActivitiesUseCase;
import kanban.flowanalysis.adapters.boardactivity.ActivityPositionStore;
import kanban.flowanalysis.adapters.boardactivity.BoardActivitySync;
import kanban.flowanalysis.adapters.boardactivity.InMemoryActivityPositionStore;
import kanban.flowanalysis.adapters.boardactivity.PostgresActivityPositionStore;
import kanban.flowanalysis.adapters.eventstore.BoardFlowEventJson;
import kanban.flowanalysis.adapters.eventstore.EventSourcedBoardFlowRepository;
import kanban.flowanalysis.adapters.eventstore.InMemoryEventStore;
import kanban.flowanalysis.adapters.eventstore.NotifyingEventStore;
import kanban.flowanalysis.adapters.eventstore.PostgresEventStore;
import kanban.flowanalysis.adapters.projection.FlowProjection;
import kanban.flowanalysis.adapters.projection.InMemoryReadModelStore;
import kanban.flowanalysis.adapters.projection.PostgresReadModelStore;
import kanban.flowanalysis.adapters.projection.ReadModelStore;
import kanban.flowanalysis.adapters.projection.WeekCalendar;
import kanban.flowanalysis.adapters.system.SystemTimeSource;
import kanban.flowanalysis.frameworks.scheduling.FlowAnalysisBackgroundWork;
import kanban.flowanalysis.frameworks.scheduling.FlowAnalysisSchedule;
import kanban.flowanalysis.frameworks.web.FlowAnalysisRequests;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.EventStore;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;
import kanban.flowanalysis.usecases.commands.recordboardactivity.impl.RecordBoardActivityUseCase;
import kanban.flowanalysis.usecases.eventfeed.EventFeed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 「流程分析」的組裝（CA-8）：建立所有具體實作並接起來。儲存方式同樣由 kanban.storage 切換。
 * Bean 名稱一律加 flow 前綴，避免和看板管理同名而被當成覆寫。
 *
 * <p>跨情境的接線只出現在這裡：防腐層要的「讀取看板動態」由看板管理的 Use Case 提供，
 * 防腐層本身只認識兩邊的邊界（DDD-3）。Main 是最外圈、最髒的元件，不受情境之間的引用限制。
 */
@Configuration
class FlowAnalysisWiring {

    // ---------------------------------------------------------------- 儲存：PostgreSQL（schema flow_analysis）

    @Configuration
    @ConditionalOnProperty(name = "kanban.storage", havingValue = "postgres", matchIfMissing = true)
    static class FlowPostgresStorage {

        @Bean
        PostgresEventStore flowEventStore(DataSource dataSource) {
            return new PostgresEventStore(dataSource, new BoardFlowEventJson());
        }

        @Bean
        ReadModelStore flowReadModelStore(DataSource dataSource, WeekCalendar flowWeekCalendar) {
            return new PostgresReadModelStore(dataSource, flowWeekCalendar);
        }

        @Bean
        ActivityPositionStore flowActivityPositionStore(DataSource dataSource) {
            return new PostgresActivityPositionStore(dataSource);
        }
    }

    // ---------------------------------------------------------------- 儲存：記憶體

    @Configuration
    @ConditionalOnProperty(name = "kanban.storage", havingValue = "memory")
    static class FlowInMemoryStorage {

        @Bean
        InMemoryEventStore flowEventStore() {
            return new InMemoryEventStore();
        }

        @Bean
        ReadModelStore flowReadModelStore(WeekCalendar flowWeekCalendar) {
            return new InMemoryReadModelStore(flowWeekCalendar);
        }

        @Bean
        ActivityPositionStore flowActivityPositionStore() {
            return new InMemoryActivityPositionStore();
        }
    }

    // ---------------------------------------------------------------- 背景排程（kanban.flow-analysis.background）

    /** 設定成 true（預設）才打開 Spring 的排程並建立 FlowAnalysisSchedule；測試裡設成 false，由測試自己呼叫。 */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "kanban.flow-analysis.background", havingValue = "true", matchIfMissing = true)
    static class FlowBackgroundSchedule {

        @Bean
        FlowAnalysisSchedule flowAnalysisSchedule(FlowAnalysisBackgroundWork work) {
            return new FlowAnalysisSchedule(work);
        }
    }

    // ---------------------------------------------------------------- 兩種儲存共用的接線

    @Bean
    TimeSource flowTimeSource() {
        return new SystemTimeSource();
    }

    /** 統計方式 S1：「一週」依畫面顯示的時區切（週一 00:00 起算）。 */
    @Bean
    WeekCalendar flowWeekCalendar(@Value("${kanban.display-zone:Asia/Taipei}") String displayZone) {
        return new WeekCalendar(ZoneId.of(displayZone));
    }

    /** Projection：流程分析讀取模型唯一的寫入途徑。 */
    @Bean
    FlowProjection flowProjection(EventFeed flowEventFeed, ReadModelStore flowReadModelStore) {
        return new FlowProjection(flowEventFeed, flowReadModelStore);
    }

    /** 指令端的看板流程存取：事件寫入成功後，同一個請求內叫 Projection 補到最新（第二階段設計 7.6 情況 2）。 */
    @Bean
    BoardFlowRepository flowBoardFlowRepository(EventStore flowEventStore, FlowProjection flowProjection) {
        return new EventSourcedBoardFlowRepository(new NotifyingEventStore(flowEventStore, flowProjection::catchUp));
    }

    /** 防腐層的同步：讀看板管理公布的看板動態，翻成流程分析的用語，交給「記錄看板動態」。 */
    @Bean
    BoardActivitySync flowBoardActivitySync(BoardActivityLogStore boardActivityLogStore,
                                            BoardFlowRepository flowBoardFlowRepository,
                                            ActivityPositionStore flowActivityPositionStore) {
        return new BoardActivitySync(
                output -> new ReadBoardActivitiesUseCase(boardActivityLogStore, output),
                output -> new RecordBoardActivityUseCase(flowBoardFlowRepository, output),
                flowActivityPositionStore);
    }

    @Bean
    FlowAnalysisRequests flowAnalysisRequests(BoardFlowRepository flowBoardFlowRepository,
                                              ReadModelStore flowReadModelStore, TimeSource flowTime,
                                              @Value("${kanban.display-zone:Asia/Taipei}") String displayZone) {
        return new FlowAnalysisRequestAssembly(flowBoardFlowRepository, flowReadModelStore, flowTime,
                ZoneId.of(displayZone));
    }

    @Bean
    FlowAnalysisBackgroundWork flowAnalysisBackgroundWork(BoardActivitySync flowBoardActivitySync,
                                                          BoardFlowRepository flowBoardFlowRepository,
                                                          ReadModelStore flowReadModelStore, TimeSource flowTime) {
        return new FlowAnalysisBackgroundAssembly(flowBoardActivitySync, flowBoardFlowRepository, flowReadModelStore,
                flowTime);
    }

    /**
     * 啟動時把流程分析的讀取模型補到最新（ES-8）。補不上不讓應用程式起不來：事件紀錄完好、指令照常可用，
     * 讀取模型停在出問題的那一筆之前，錯誤寫進日誌（同看板管理的 startupCatchUp）。
     */
    @Bean
    ApplicationRunner flowStartupCatchUp(FlowProjection flowProjection) {
        return arguments -> {
            try {
                flowProjection.catchUp();
            } catch (RuntimeException failure) {
                System.getLogger(FlowAnalysisWiring.class.getName()).log(System.Logger.Level.ERROR,
                        "啟動時流程分析的讀取模型沒有補到最新；流程分析頁會看到舊資料，請依錯誤內容處理後重新啟動", failure);
            }
        };
    }
}
