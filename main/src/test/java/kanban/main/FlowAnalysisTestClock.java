package kanban.main;

import java.time.Duration;
import java.time.Instant;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** 測試用：可以往後調的流程分析時間（例如調到 6 天後，看卡住判斷）。看板管理的時間不受影響。 */
@TestConfiguration(proxyBeanMethods = false)
class FlowAnalysisTestClock {

    static final class AdjustableTime implements TimeSource {

        private volatile Duration offset = Duration.ZERO;

        @Override
        public Instant now() {
            return Instant.now().plus(offset);
        }

        void advance(Duration duration) {
            offset = offset.plus(duration);
        }

        void reset() {
            offset = Duration.ZERO;
        }
    }

    @Bean
    @Primary
    AdjustableTime adjustableFlowTime() {
        return new AdjustableTime();
    }
}
