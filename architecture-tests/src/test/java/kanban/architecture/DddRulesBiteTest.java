package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.DddRules;
import org.junit.jupiter.api.Test;

/** 證明每條 DDD 規則遇到違規樣本真的會失敗。 */
class DddRulesBiteTest {

    @Test
    void DDD8_Repository實作放在UseCases圈會被擋下() {
        assertBites(DddRules.REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE, "repositoryplacement");
    }

    @Test
    void DDD8_Repository介面放在Adapters圈會被擋下() {
        assertBites(DddRules.REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE, "repositoryinterfaceplacement");
    }

    @Test
    void DDD2_另一個情境引用看板的領域類別會被擋下() {
        assertBites(DddRules.contextsDoNotShareDomain("kanban.fixtures.contexts"), "contexts");
    }
}
