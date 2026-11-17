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
        // 第二個業務情境的聚合測試也讀得到（architecture-tests 的 pom 有它的 test-jar）
        assertThat(Imported.entitiesTests().contain("kanban.flowanalysis.entities.boardflow.BoardFlowScenario"))
                .as("流程分析的聚合測試也要檢查").isTrue();
        assertThat(Imported.entitiesTests().contain("kanban.flowanalysis.entities.boardflow.BoardFlow"))
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
        // 第二階段起有兩個聚合（entities.board 的看板、entities.boardflow 的看板流程），各自是一塊，不再需要允許沒有對象
        EntitiesRules.AGGREGATE_REFERENCES_BY_ID.check(Imported.production());
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
