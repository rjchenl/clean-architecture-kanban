package kanban.architecture;

import kanban.architecture.rules.EventSourcingRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** 對正式程式碼跑 Event Sourcing 規則。 */
class EventSourcingRulesTest {

    @Test
    void ES1_寫入端只有事件紀錄() {
        EventSourcingRules.NO_STATE_TABLE_ABSTRACTION_ON_WRITE_SIDE.check(Imported.production());
    }
}
