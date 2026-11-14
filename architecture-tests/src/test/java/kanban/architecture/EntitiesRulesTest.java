package kanban.architecture;

import kanban.architecture.rules.EntitiesRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** 對正式程式碼跑 Entities 圈規則。 */
class EntitiesRulesTest {

    @Test
    void DDD6_DDD7_值物件與事件是record() {
        EntitiesRules.VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS.check(Imported.production());
    }

    @Test
    void ES4_不讀時鐘不產生亂數() {
        EntitiesRules.NO_CLOCK_OR_RANDOM_IN_ENTITIES.check(Imported.production());
    }
}
