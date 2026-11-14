package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.CqrsRules;
import org.junit.jupiter.api.Test;

/** 證明每條 CQRS 規則遇到違規樣本真的會失敗。 */
class CqrsRulesBiteTest {

    @Test
    void CQRS1_指令UseCase引用查詢端的模型會被擋下() {
        assertBites(CqrsRules.COMMANDS_AND_QUERIES_SEPARATE, "commandusesquery");
    }

    @Test
    void CQRS3_查詢UseCase載入聚合會被擋下() {
        assertBites(CqrsRules.QUERIES_NEVER_TOUCH_WRITE_SIDE, "queryloadsaggregate");
    }

    @Test
    void CQRS3_查詢UseCase讀事件紀錄會被擋下() {
        assertBites(CqrsRules.QUERIES_NEVER_TOUCH_WRITE_SIDE, "queryreadsevents");
    }

    @Test
    void CQRS5_Controller直接寫讀取模型會被擋下() {
        assertBites(CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL, "readmodelwrite");
    }

    @Test
    void CQRS5_同套件裡不是Projection的類別寫讀取模型會被擋下() {
        assertBites(CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL, "readmodelsidewrite");
    }

    @Test
    void CQRS4_指令UseCase用讀取模型做決定會被擋下() {
        assertBites(CqrsRules.COMMANDS_NEVER_READ_READ_MODEL, "commandreadsreadmodel");
    }
}
