package kanban.architecture;

import kanban.architecture.rules.EventSourcingRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** 對正式程式碼跑 Event Sourcing 規則。 */
class EventSourcingRulesTest {

    @Test
    void ES1_只有事件庫與讀取模型的資料庫版使用JDBC() {
        EventSourcingRules.JDBC_ONLY_IN_POSTGRES_STORES.check(Imported.production());
    }

    @Test
    void ES1_寫入端只有事件紀錄() {
        EventSourcingRules.NO_STATE_TABLE_ABSTRACTION_ON_WRITE_SIDE.check(Imported.production());
    }
}
