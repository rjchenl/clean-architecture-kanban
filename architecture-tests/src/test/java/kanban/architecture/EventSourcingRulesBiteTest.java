package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.EventSourcingRules;
import org.junit.jupiter.api.Test;

/** 證明每條 Event Sourcing 規則遇到違規樣本真的會失敗。 */
class EventSourcingRulesBiteTest {

    @Test
    void ES1_指令端多一個存現況的Gateway會被擋下() {
        assertBites(EventSourcingRules.NO_STATE_TABLE_ABSTRACTION_ON_WRITE_SIDE, "statetable");
    }

    @Test
    void ES1_Repository直接碰資料庫會被擋下() {
        assertBites(EventSourcingRules.NO_STATE_TABLE_ABSTRACTION_ON_WRITE_SIDE, "staterepository");
    }
}
