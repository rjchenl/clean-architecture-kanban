package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 儲存換成記憶體版，整套流程照樣走得通——不需要資料庫（CA-6：儲存方式是可以抽換的細節）。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "kanban.storage=memory")
class InMemoryModeIntegrationTest extends HttpTestSupport {

    @Test
    void 記憶體模式跑完整流程() {
        String board = createBoard("記憶體看板");
        String todo = addColumn(board, "待辦");
        String doing = addColumn(board, "進行中");
        put("/api/boards/" + board + "/columns/" + doing + "/wip-limit", "{\"limit\":1}");
        String first = addCard(board, todo, "第一張");
        String second = addCard(board, todo, "第二張");
        String moveTo = "{\"columnId\":\"" + doing + "\",\"position\":0}";

        Reply moved = put("/api/boards/" + board + "/cards/" + first + "/column", moveTo);
        Reply rejected = put("/api/boards/" + board + "/cards/" + second + "/column", moveTo);

        assertThat(moved.status()).isEqualTo(200);
        assertThat(rejected.status()).isEqualTo(422);
        assertThat(rejected.text("message")).isEqualTo("這一欄已達在製品上限，放不進去");
        Reply view = get("/api/boards/" + board);
        assertThat(view.body().get("columns").get(1).get("cardCountLabel").asString()).isEqualTo("1 / 1");
        assertThat(get("/api/boards/" + board + "/cards/" + first + "/history").body().get("lines")).hasSize(2);
    }
}
