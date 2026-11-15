package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.time.Instant;
import kanban.boardmanagement.adapters.projection.ReadModelStore;
import kanban.boardmanagement.entities.board.Board;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.ConcurrentUpdateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** CQRS-6：設計文件 7.5 節寫的三種情況，各有一支測試證明真的如描述般運作。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsistencyIntegrationTest extends HttpTestSupport {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        PostgresContainer.register(registry);
    }

    @MockitoSpyBean
    ReadModelStore readModel;

    @Autowired
    BoardRepository boards;

    @AfterEach
    void repairReadModel() {
        reset(readModel);
    }

    @Test
    void 情況1_指令成功後立刻查詢_看得到新資料() {
        String board = createBoard("一致性");

        Reply renamed = put("/api/boards/" + board + "/name", "{\"name\":\"改名後\"}");
        Reply view = get("/api/boards/" + board);

        assertThat(renamed.status()).isEqualTo(200);
        assertThat(view.text("name")).isEqualTo("改名後");
    }

    @Test
    void 情況2_讀取模型更新失敗_指令仍成功_查詢是舊資料_下一次指令後補上() {
        String board = createBoard("原本的名稱");
        // 讓下一次更新讀取模型失敗一次，之後恢復正常
        doThrow(new IllegalStateException("讀取模型故障")).doCallRealMethod().when(readModel).saveBoard(any());

        Reply renamed = put("/api/boards/" + board + "/name", "{\"name\":\"新的名稱\"}");

        assertThat(renamed.status()).as("事件已經存好，指令仍算成功").isEqualTo(200);
        assertThat(renamed.text("outcome")).isEqualTo("SUCCEEDED");
        assertThat(get("/api/boards/" + board).text("name")).as("讀取模型還沒補上，看到的是舊資料").isEqualTo("原本的名稱");

        Reply next = post("/api/boards/" + board + "/columns", "{\"name\":\"待辦\"}");

        assertThat(next.status()).isEqualTo(200);
        Reply view = get("/api/boards/" + board);
        assertThat(view.text("name")).as("下一次指令成功後，先前落後的也一起補上").isEqualTo("新的名稱");
        assertThat(view.body().get("columns")).hasSize(1);
    }

    @Test
    void 情況3_兩個請求同時改同一個看板_後存的被拒絕() {
        String boardId = createBoard("搶著改");
        Board first = boards.load(new BoardId(boardId)).orElseThrow();
        Board second = boards.load(new BoardId(boardId)).orElseThrow();
        first.addColumn(new ColumnId("col-a"), new ColumnName("甲"), Instant.now());
        second.addColumn(new ColumnId("col-b"), new ColumnName("乙"), Instant.now());

        boards.save(first);

        assertThatThrownBy(() -> boards.save(second)).isInstanceOf(ConcurrentUpdateException.class);
        Reply view = get("/api/boards/" + boardId);
        assertThat(view.body().get("columns")).hasSize(1);
        assertThat(view.body().get("columns").get(0).get("name").asString()).isEqualTo("甲");
    }
}
