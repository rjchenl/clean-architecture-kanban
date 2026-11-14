package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/** 整套系統接上 PostgreSQL，用真正的 HTTP 走使用者會走的流程。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BoardHttpIntegrationTest extends HttpTestSupport {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresContainer.register(registry);
    }

    private static JsonNode column(Reply board, int index) {
        return board.body().get("columns").get(index);
    }

    @Test
    void 完整流程_超過在製品上限會被拒絕_歷程查得到() {
        String board = createBoard("產品開發");
        String todo = addColumn(board, "待辦");
        String doing = addColumn(board, "進行中");
        assertThat(put("/api/boards/" + board + "/columns/" + doing + "/wip-limit", "{\"limit\":1}").status())
                .isEqualTo(200);
        String first = addCard(board, todo, "設計登入頁");
        String second = addCard(board, todo, "寫用語表");

        Reply moved = put("/api/boards/" + board + "/cards/" + first + "/column",
                "{\"columnId\":\"" + doing + "\",\"position\":0}");
        Reply rejected = put("/api/boards/" + board + "/cards/" + second + "/column",
                "{\"columnId\":\"" + doing + "\",\"position\":0}");

        assertThat(moved.status()).isEqualTo(200);
        assertThat(moved.text("outcome")).isEqualTo("SUCCEEDED");
        assertThat(rejected.status()).isEqualTo(422);
        assertThat(rejected.text("outcome")).isEqualTo("REJECTED");
        assertThat(rejected.text("message")).isEqualTo("這一欄已達在製品上限，放不進去");

        Reply view = get("/api/boards/" + board);
        assertThat(view.status()).isEqualTo(200);
        assertThat(view.text("name")).isEqualTo("產品開發");
        assertThat(column(view, 0).get("name").asString()).isEqualTo("待辦");
        assertThat(column(view, 0).get("cardCountLabel").asString()).isEqualTo("1");
        assertThat(column(view, 1).get("cardCountLabel").asString()).isEqualTo("1 / 1");
        assertThat(column(view, 1).get("cards").get(0).get("title").asString()).isEqualTo("設計登入頁");

        Reply history = get("/api/boards/" + board + "/cards/" + first + "/history");
        assertThat(history.status()).isEqualTo(200);
        assertThat(history.text("cardTitle")).isEqualTo("設計登入頁");
        assertThat(history.body().get("lines")).hasSize(2);
        assertThat(history.body().get("lines").get(0).get("description").asString()).isEqualTo("新增到「待辦」");
        assertThat(history.body().get("lines").get(1).get("description").asString()).isEqualTo("從「待辦」移到「進行中」");
        assertThat(history.body().get("lines").get(1).get("occurredAt").asString())
                .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}");
    }

    @Test
    void 每一種操作都走得通() {
        String board = createBoard("操作總覽");
        String todo = addColumn(board, "待辦");
        String doing = addColumn(board, "進行中");
        String card = addCard(board, todo, "第一張");
        String other = addCard(board, todo, "第二張");
        String base = "/api/boards/" + board;

        assertThat(put(base + "/name", "{\"name\":\"操作總覽（改）\"}").status()).isEqualTo(200);
        assertThat(put(base + "/columns/" + todo + "/name", "{\"name\":\"準備中\"}").status()).isEqualTo(200);
        assertThat(put(base + "/columns/" + doing + "/position", "{\"position\":0}").status()).isEqualTo(200);
        assertThat(put(base + "/columns/" + doing + "/wip-limit", "{\"limit\":3}").status()).isEqualTo(200);
        assertThat(delete(base + "/columns/" + doing + "/wip-limit").status()).isEqualTo(200);
        assertThat(put(base + "/cards/" + card + "/content", "{\"title\":\"第一張（改）\",\"description\":\"說明\"}")
                .status()).isEqualTo(200);
        assertThat(put(base + "/cards/" + other + "/position", "{\"position\":0}").status()).isEqualTo(200);
        assertThat(delete(base + "/cards/" + other).status()).isEqualTo(200);
        assertThat(put(base + "/cards/" + card + "/column", "{\"columnId\":\"" + doing + "\",\"position\":0}")
                .status()).isEqualTo(200);
        assertThat(delete(base + "/columns/" + todo).status()).as("卡片都移走了，可以刪欄").isEqualTo(200);

        Reply view = get(base);
        assertThat(view.text("name")).isEqualTo("操作總覽（改）");
        assertThat(view.body().get("columns")).hasSize(1);
        assertThat(column(view, 0).get("name").asString()).isEqualTo("進行中");
        assertThat(column(view, 0).get("cardCountLabel").asString()).isEqualTo("1");
        assertThat(column(view, 0).get("cards").get(0).get("title").asString()).isEqualTo("第一張（改）");

        assertThat(post(base + "/archive", null).status()).isEqualTo(200);
        Reply afterArchive = put(base + "/name", "{\"name\":\"封存後改名\"}");
        assertThat(afterArchive.status()).isEqualTo(422);
        assertThat(afterArchive.text("message")).isEqualTo("看板已封存，不能再變更");
        assertThat(get(base).text("archivedLabel")).isEqualTo("已封存");
    }

    @Test
    void 看板清單列出建立的看板() {
        String board = createBoard("清單裡找得到我");

        Reply list = get("/api/boards");

        assertThat(list.status()).isEqualTo(200);
        assertThat(list.body().get("boards").valueStream().map(item -> item.get("boardId").asString())).contains(board);
    }

    @Test
    void 刪除還有卡片的欄_422() {
        String board = createBoard("刪欄測試");
        String todo = addColumn(board, "待辦");
        addCard(board, todo, "卡片");

        Reply reply = delete("/api/boards/" + board + "/columns/" + todo);

        assertThat(reply.status()).isEqualTo(422);
        assertThat(reply.text("message")).isEqualTo("欄內還有卡片，不能刪除");
    }

    @Test
    void 不存在的看板_指令與查詢都是404() {
        Reply command = post("/api/boards/no-such-board/columns", "{\"name\":\"待辦\"}");
        Reply query = get("/api/boards/no-such-board");
        Reply history = get("/api/boards/no-such-board/cards/no-such-card/history");

        assertThat(command.status()).isEqualTo(404);
        assertThat(command.text("message")).isEqualTo("找不到這個看板");
        assertThat(query.status()).isEqualTo(404);
        assertThat(query.body().get("found").asBoolean()).isFalse();
        assertThat(history.status()).isEqualTo(404);
    }

    @Test
    void 請求內容缺欄位或讀不懂_422且有中文訊息() {
        String board = createBoard("壞請求測試");
        String todo = addColumn(board, "待辦");
        String card = addCard(board, todo, "卡片");
        String moveCard = "/api/boards/" + board + "/cards/" + card + "/column";

        Reply emptyJson = post("/api/boards", "{}");
        Reply notJson = post("/api/boards", "這不是 JSON");
        Reply noBody = post("/api/boards", null);
        Reply positionIsText = put(moveCard, "{\"columnId\":\"" + todo + "\",\"position\":\"abc\"}");
        Reply positionMissing = put(moveCard, "{\"columnId\":\"" + todo + "\"}");
        Reply positionNegative = put(moveCard, "{\"columnId\":\"" + todo + "\",\"position\":-1}");

        for (Reply reply : new Reply[] {emptyJson, notJson, noBody, positionIsText, positionMissing}) {
            assertThat(reply.status()).isEqualTo(422);
            assertThat(reply.text("outcome")).isEqualTo("REJECTED");
            assertThat(reply.text("message")).isEqualTo("請求內容不完整或格式不正確");
        }
        assertThat(positionNegative.status()).isEqualTo(422);
        assertThat(positionNegative.text("message")).isEqualTo("位置超出範圍");
    }

    @Test
    void 名稱空白_422且訊息出自Presenter() {
        Reply reply = post("/api/boards", "{\"name\":\"   \"}");

        assertThat(reply.status()).isEqualTo(422);
        assertThat(reply.text("message")).isEqualTo("名稱不可空白");
    }
}
