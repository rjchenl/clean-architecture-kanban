package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.DddRules;
import org.junit.jupiter.api.Test;

/** 證明每條 DDD 規則遇到違規樣本真的會失敗。 */
class DddRulesBiteTest {

    @Test
    void DDD2_另一個情境引用看板的領域類別會被擋下() {
        assertBites(DddRules.contextsDoNotShareDomain("kanban.fixtures.contexts"), "contexts");
    }
}
