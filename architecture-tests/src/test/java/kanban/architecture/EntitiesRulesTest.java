package kanban.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.architecture.rules.EntitiesRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** 對正式程式碼跑 Entities 圈規則。 */
class EntitiesRulesTest {

    @Test
    void ES5_檢查對象是Entities圈的全部測試程式_不看檔名也不含正式程式碼() {
        assertThat(Imported.entitiesTests().contain("kanban.boardmanagement.entities.board.BoardScenario"))
                .as("檔名不以 Test 結尾的測試程式也要檢查").isTrue();
        assertThat(Imported.entitiesTests().contain("kanban.boardmanagement.entities.board.Board"))
                .as("正式程式碼不屬於聚合測試").isFalse();
    }

    @Test
    void DDD6_DDD7_值物件與事件是record() {
        EntitiesRules.VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS.check(Imported.production());
    }

    @Test
    void DDD7_事件用過去式命名() {
        EntitiesRules.EVENTS_NAMED_IN_PAST_TENSE.check(Imported.production());
    }

    @Test
    void DDD4_聚合沒有setter也不外露內部物件() {
        EntitiesRules.AGGREGATE_HAS_NO_SETTERS.check(Imported.production());
    }

    @Test
    void DDD4_聚合內部實體不是public() {
        EntitiesRules.AGGREGATE_INTERNALS_NOT_PUBLIC.check(Imported.production());
    }

    @Test
    void ES3_套用事件的方法不檢查不拒絕() {
        EntitiesRules.APPLY_METHODS_DO_NOT_REJECT.check(Imported.production());
    }

    @Test
    void ES3_狀態只在套用事件時改變() {
        EntitiesRules.STATE_CHANGES_ONLY_IN_APPLY.check(Imported.production());
    }

    @Test
    void DDD5_聚合之間不互相持有物件() {
        // 目前只有一個聚合（Board），允許沒有可比較的對象
        EntitiesRules.AGGREGATE_REFERENCES_BY_ID.allowEmptyShould(true).check(Imported.production());
    }

    @Test
    void ES5_聚合測試只透過BoardScenario() {
        EntitiesRules.AGGREGATE_TESTS_USE_SCENARIO.check(Imported.entitiesTests());
    }

    @Test
    void ES4_不讀時鐘不產生亂數() {
        EntitiesRules.NO_CLOCK_OR_RANDOM_IN_ENTITIES.check(Imported.production());
    }
}
