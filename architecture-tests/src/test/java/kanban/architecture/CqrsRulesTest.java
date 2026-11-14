package kanban.architecture;

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
    void CQRS4_寫入端不讀讀取模型() {
        CqrsRules.COMMANDS_NEVER_READ_READ_MODEL.check(Imported.production());
    }
}
