package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 背景排程真的接上了：只有這支測試打開排程（同步間隔縮成 0.2 秒），跑完就把整個應用程式關掉，
 * 不會在背景繼續同步、干擾之後的測試。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"kanban.flow-analysis.background=true", "kanban.flow-analysis.sync-interval=PT0.2S"})
@DirtiesContext
class BackgroundScheduleIntegrationTest extends FlowAnalysisTestSupport {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresContainer.register(registry);
    }

    @Test
    void 看板上操作後_不手動同步_幾秒內流程分析看得到() throws InterruptedException {
        String board = createBoard("背景同步");
        addColumn(board, "待辦");

        Reply view = viewFlow(board);
        for (int waited = 0; waited < 10_000 && !(view.status() == 200 && columns(view).contains("待辦:0")); waited += 100) {
            Thread.sleep(100);
            view = viewFlow(board);
        }

        assertThat(view.status()).isEqualTo(200);
        assertThat(columns(view)).containsExactly("待辦:0");
    }
}
