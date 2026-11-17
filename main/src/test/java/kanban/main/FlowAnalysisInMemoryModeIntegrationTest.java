package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 不用資料庫（kanban.storage=memory）時，流程分析一樣完整可用。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "kanban.storage=memory")
class FlowAnalysisInMemoryModeIntegrationTest extends FlowAnalysisTestSupport {

    @Test
    void 記憶體模式_流程分析完整可用() {
        String board = createBoard("記憶體");
        String todo = addColumn(board, "待辦");
        String doing = addColumn(board, "進行中");
        String done = addColumn(board, "完成");
        String card = addCard(board, todo, "串接金流");
        moveCard(board, card, doing);
        moveCard(board, card, done);
        sync();
        setStage(board, todo, "NOT_STARTED");
        setStage(board, doing, "IN_PROGRESS");
        assertThat(setStage(board, done, "DONE").status()).isEqualTo(200);

        Reply view = viewFlow(board);

        assertThat(view.status()).isEqualTo(200);
        assertThat(columns(view)).containsExactly("待辦:0", "進行中:0", "完成:1");
        assertThat(view.text("deliverySummary")).startsWith("最近 8 週：完成 1 張");
    }
}
