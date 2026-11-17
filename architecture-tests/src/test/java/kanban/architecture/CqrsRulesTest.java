package kanban.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.architecture.rules.CqrsRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** 對正式程式碼跑 CQRS 規則。 */
class CqrsRulesTest {

    @Test
    void CQRS1_指令端不引用查詢端() {
        CqrsRules.COMMANDS_AND_QUERIES_SEPARATE.check(Imported.production());
    }

    @Test
    void CQRS3_查詢端不碰寫入端() {
        CqrsRules.QUERIES_NEVER_TOUCH_WRITE_SIDE.check(Imported.production());
    }

    @Test
    void CQRS5_讀取模型只由Projection更新() {
        CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL.check(Imported.production());
    }

    @Test
    void CQRS5_每個讀取模型儲存都被認到() {
        // 規則靠「名稱或形狀」認出讀取模型的儲存；這裡確認每一份真的都被認到，換個名字也不會漏檢
        assertThat(CqrsRules.readModelStoresIn(Imported.production())).contains(
                "kanban.boardmanagement.adapters.projection.ReadModelStore",
                "kanban.boardmanagement.adapters.projection.InMemoryReadModelStore",
                "kanban.boardmanagement.adapters.projection.BoardActivityLogStore",
                "kanban.boardmanagement.adapters.projection.InMemoryBoardActivityLog",
                "kanban.flowanalysis.adapters.projection.ReadModelStore",
                "kanban.flowanalysis.adapters.projection.InMemoryReadModelStore");
    }

    @Test
    void CQRS4_寫入端不讀讀取模型() {
        CqrsRules.COMMANDS_NEVER_READ_READ_MODEL.check(Imported.production());
    }
}
