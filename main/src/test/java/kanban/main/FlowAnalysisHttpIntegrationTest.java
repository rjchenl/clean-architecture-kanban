package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/** 流程分析的 HTTP 端點：真的啟動應用程式、接真的 PostgreSQL，用 HTTP 打。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlowAnalysisHttpIntegrationTest extends FlowAnalysisTestSupport {

    private static final String INVALID_REQUEST = "請求內容不完整或格式不正確";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresContainer.register(registry);
    }

    @Test
    void 完整流程_設定階段與門檻_數字正確() {
        String board = createBoard("產品開發");
        String todo = addColumn(board, "待辦");
        String doing = addColumn(board, "進行中");
        String done = addColumn(board, "完成");
        String card = addCard(board, todo, "串接金流");
        addCard(board, todo, "寫文件");
        moveCard(board, card, doing);
        moveCard(board, card, done);
        sync();
        assertThat(setStage(board, todo, "NOT_STARTED").status()).isEqualTo(200);
        assertThat(setStage(board, doing, "IN_PROGRESS").status()).isEqualTo(200);
        assertThat(setStage(board, done, "DONE").status()).isEqualTo(200);
        assertThat(setThreshold(board, 5).status()).isEqualTo(200);

        Reply view = viewFlow(board);

        assertThat(view.status()).isEqualTo(200);
        assertThat(view.text("boardName")).isEqualTo("產品開發");
        List<String> columns = new ArrayList<>();
        for (JsonNode column : view.body().get("columns")) {
            columns.add(column.get("name").asString() + ":" + column.get("stageLabel").asString() + ":"
                    + column.get("workItemCount").asString());
        }
        assertThat(columns).containsExactly("待辦:未開始:1", "進行中:進行中:0", "完成:完成:1");
        assertThat(view.text("deliverySummary")).isEqualTo("最近 8 週：完成 1 張，交付時間平均 0.0 天、最長 0.0 天");
        assertThat(view.text("stuckThresholdLabel")).isEqualTo("卡住門檻 5 天");

        Reply removed = delete(thresholdPath(board));

        assertThat(removed.status()).isEqualTo(200);
        assertThat(viewFlow(board).text("stuckThresholdLabel")).isEqualTo("沒有設定卡住門檻");
    }

    @Test
    void 還沒同步的看板_404且有中文訊息() {
        String board = createBoard("剛建立");

        Reply view = viewFlow(board);

        assertThat(view.status()).isEqualTo(404);
        assertThat(view.body().get("found").asBoolean()).isFalse();
        assertThat(view.text("notFoundMessage")).isEqualTo("流程分析還沒準備好，請稍後重新整理");
    }

    @Test
    void 請求內容缺欄位或讀不懂_422且有中文訊息() {
        String board = createBoard("格式");
        String todo = addColumn(board, "待辦");
        sync();

        List<Reply> replies = List.of(
                put(stagePath(board, todo), "{}"),
                put(stagePath(board, todo), "不是 JSON"),
                put(thresholdPath(board), null),
                put(thresholdPath(board), "{}"));

        assertThat(replies).allSatisfy(reply -> {
            assertThat(reply.status()).isEqualTo(422);
            assertThat(reply.text("message")).isEqualTo(INVALID_REQUEST);
        });
    }

    @Test
    void 階段字串不認得_422() {
        String board = createBoard("階段");
        String todo = addColumn(board, "待辦");
        sync();

        Reply reply = setStage(board, todo, "FINISHED");

        assertThat(reply.status()).isEqualTo(422);
        assertThat(reply.text("message")).isEqualTo("不認得這個階段");
    }

    @Test
    void 門檻0與366與非數字_422() {
        String board = createBoard("門檻");
        sync();

        Reply zero = setThreshold(board, 0);
        Reply tooLong = setThreshold(board, 366);
        Reply notNumber = put(thresholdPath(board), "{\"days\":\"五天\"}");

        assertThat(List.of(zero, tooLong)).allSatisfy(reply -> {
            assertThat(reply.status()).isEqualTo(422);
            assertThat(reply.text("message")).isEqualTo("卡住門檻要是 1 到 365 天");
        });
        assertThat(notNumber.status()).isEqualTo(422);
        assertThat(notNumber.text("message")).isEqualTo(INVALID_REQUEST);
    }

    @Test
    void 網址裡的識別碼含控制字元_不是500() {
        Reply nul = get("/api/flow-analysis/boards/board%00x");
        Reply command = setThreshold("board%01x", 5);
        Reply query = get("/api/flow-analysis/boards/board%01x");

        // 空字元：伺服器在進到應用程式之前就擋下
        assertThat(nul.status()).isEqualTo(400);
        // 其他控制字元：由流程分析自己拒絕或回「還沒準備好」
        assertThat(command.status()).isEqualTo(422);
        assertThat(command.text("message")).isEqualTo("內容含有不能使用的字元");
        assertThat(query.status()).isEqualTo(404);
        assertThat(query.text("notFoundMessage")).isEqualTo("流程分析還沒準備好，請稍後重新整理");
    }

    @Test
    void 看板管理的端點_請求讀不懂時仍回看板管理的訊息() {
        Reply reply = post("/api/boards", "不是 JSON");

        assertThat(reply.status()).isEqualTo(422);
        assertThat(reply.text("outcome")).isEqualTo("REJECTED");
        assertThat(reply.text("message")).isEqualTo(INVALID_REQUEST);
    }
}
