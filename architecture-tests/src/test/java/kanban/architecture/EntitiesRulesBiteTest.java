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
    void DDD7_現在式命名的事件會被擋下() {
        assertBites(EntitiesRules.EVENTS_NAMED_IN_PAST_TENSE, "eventnaming");
    }

    @Test
    void DDD4_公開setter會被擋下() {
        assertBites(EntitiesRules.AGGREGATE_HAS_NO_SETTERS, "setter");
    }

    @Test
    void DDD4_把內部清單交出去會被擋下() {
        assertBites(EntitiesRules.AGGREGATE_HAS_NO_SETTERS, "leakyaggregate");
    }

    @Test
    void DDD4_公開的內部實體會被擋下() {
        assertBites(EntitiesRules.AGGREGATE_INTERNALS_NOT_PUBLIC, "publicinternals");
    }

    @Test
    void ES3_在套用方法裡丟例外會被擋下() {
        assertBites(EntitiesRules.APPLY_METHODS_DO_NOT_REJECT, "applyrejects");
    }

    @Test
    void ES3_在套用方法裡呼叫規則檢查會被擋下() {
        assertBites(EntitiesRules.APPLY_METHODS_DO_NOT_REJECT, "applyrequires");
    }

    @Test
    void ES3_指令方法直接改狀態會被擋下() {
        assertBites(EntitiesRules.STATE_CHANGES_ONLY_IN_APPLY, "directstate");
    }

    @Test
    void DDD5_聚合互相持有物件會被擋下() {
        assertBites(EntitiesRules.AGGREGATE_REFERENCES_BY_ID, "aggregates");
    }

    @Test
    void ES5_聚合測試直接重播事件會被擋下() {
        assertBites(EntitiesRules.AGGREGATE_TESTS_USE_SCENARIO, "scenario");
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
