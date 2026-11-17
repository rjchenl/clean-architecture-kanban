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

    /**
     * ES-5 的檢查對象不能漏掉任何業務情境：正式程式碼裡有 entities 圈的情境，它的聚合測試都要讀得到。
     * 新增業務情境時忘了把 entities 的 test-jar 加進 architecture-tests 的 pom，這裡會失敗，
     * 而不是讓 AGGREGATE_TESTS_USE_SCENARIO 默默看不到新情境的聚合測試。依形狀找情境，不列情境名稱。
     */
    @Test
    void ES5_每個有Entities圈的業務情境_它的聚合測試都讀得到() {
        assertThat(contextsWithEntities(Imported.entitiesTests()))
                .as("這些業務情境有 entities 圈，但 architecture-tests 讀不到它們的聚合測試（pom 少了 entities 的 test-jar）")
                .containsAll(contextsWithEntities(Imported.production()));
    }

    private static java.util.Set<String> contextsWithEntities(com.tngtech.archunit.core.domain.JavaClasses classes) {
        return classes.stream()
                .map(com.tngtech.archunit.core.domain.JavaClass::getPackageName)
                .filter(name -> name.matches("kanban\\.[a-z]+\\.entities(\\..+)?"))
                .map(name -> name.split("\\.")[1])
                .collect(java.util.stream.Collectors.toSet());
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
