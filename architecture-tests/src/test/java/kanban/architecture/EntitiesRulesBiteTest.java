package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.EntitiesRules;
import org.junit.jupiter.api.Test;

/** 證明每條 Entities 圈規則遇到違規樣本真的會失敗。 */
class EntitiesRulesBiteTest {

    @Test
    void DDD6_可變的值物件會被擋下() {
        assertBites(EntitiesRules.VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS, "mutablevalue");
    }

    @Test
    void ES4_讀系統時間會被擋下() {
        assertBites(EntitiesRules.NO_CLOCK_OR_RANDOM_IN_ENTITIES, "clock");
    }

    @Test
    void ES4_產生亂數識別碼會被擋下() {
        assertBites(EntitiesRules.NO_CLOCK_OR_RANDOM_IN_ENTITIES, "randomid");
    }
}
