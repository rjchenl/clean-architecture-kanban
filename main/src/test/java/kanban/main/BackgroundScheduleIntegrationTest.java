package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 背景排程真的接上了：只有這支測試打開排程（同步與檢查卡住的間隔都縮成 0.2 秒），跑完就把整個應用程式關掉，
 * 不會在背景繼續同步、干擾之後的測試。兩支測試都不手動呼叫背景工作，只等排程自己做。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"kanban.flow-analysis.background=true", "kanban.flow-analysis.sync-interval=PT0.2S",
                "kanban.flow-analysis.stuck-check-interval=PT0.2S"})
@Import(FlowAnalysisTestClock.class)
@DirtiesContext
class BackgroundScheduleIntegrationTest extends FlowAnalysisTestSupport {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresContainer.register(registry);
    }

    @Autowired
    FlowAnalysisTestClock.AdjustableTime flowTime;

    @AfterEach
    void restoreClock() {
        flowTime.reset();
    }

    /** 最多等 10 秒，每 100 毫秒看一次流程分析頁。 */
    private Reply waitForFlow(String boardId, Predicate<Reply> condition) throws InterruptedException {
        Reply view = viewFlow(boardId);
        for (int waited = 0; waited < 10_000 && !condition.test(view); waited += 100) {
            Thread.sleep(100);
            view = viewFlow(boardId);
        }
        return view;
    }

    @Test
    void 看板上操作後_不手動同步_幾秒內流程分析看得到() throws InterruptedException {
        String board = createBoard("背景同步");
        addColumn(board, "待辦");

        Reply view = waitForFlow(board, reply -> reply.status() == 200 && columns(reply).contains("待辦:0"));

        assertThat(view.status()).isEqualTo(200);
        assertThat(columns(view)).containsExactly("待辦:0");
    }

    @Test
    void 待滿門檻後_不手動檢查_定時檢查會列入卡住() throws InterruptedException {
        String board = createBoard("背景檢查卡住");
        String coding = addColumn(board, "開發中");
        addCard(board, coding, "串接金流");
        waitForFlow(board, reply -> reply.status() == 200 && columns(reply).contains("開發中:1"));
        assertThat(setStage(board, coding, "IN_PROGRESS").status()).isEqualTo(200);
        assertThat(setThreshold(board, 5).status()).isEqualTo(200);

        flowTime.advance(Duration.ofDays(6));
        Reply view = waitForFlow(board, reply -> stuckTitles(reply).contains("串接金流"));

        assertThat(stuckTitles(view)).as("定時檢查（設計 7.6 情況 3）自己把待滿門檻的工作項目列入卡住")
                .containsExactly("串接金流");
    }
}
