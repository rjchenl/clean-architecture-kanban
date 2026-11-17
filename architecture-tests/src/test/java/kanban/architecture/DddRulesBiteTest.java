package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import com.tngtech.archunit.lang.ArchRule;
import java.util.Map;
import java.util.Set;
import kanban.architecture.rules.DddRules;
import kanban.architecture.support.Imported;
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
    void DDD8_沒照名稱慣例的Gateway實作放在最外圈會被擋下() {
        assertBites(DddRules.REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE, "gatewayunnamed");
    }

    @Test
    void DDD2_另一個情境引用看板的領域類別會被擋下() {
        assertBites(DddRules.contextsDoNotShareDomain("kanban.fixtures.contexts"), "contexts");
    }

    /** 樣本用的情境關係：upstream 公布 usecases.queries.published；downstream 由 adapters.translation 翻譯。 */
    private static ArchRule sampleContextMap(String fixtureName) {
        return DddRules.contextsIntegrateOnlyThroughPublishedLanguage("kanban.fixtures." + fixtureName,
                Map.of("upstream", Set.of("usecases.queries.published")),
                Map.of("downstream", Map.of("upstream", "adapters.translation")));
    }

    @Test
    void DDD3_上游引用下游會被擋下() {
        assertBites(sampleContextMap("contextmapupward"), "contextmapupward");
    }

    @Test
    void DDD3_下游的非防腐層引用上游會被擋下() {
        assertBites(sampleContextMap("contextmapbypass"), "contextmapbypass");
    }

    @Test
    void DDD3_防腐層引用上游公布格式以外的東西會被擋下() {
        assertBites(sampleContextMap("contextmapleak"), "contextmapleak");
    }

    @Test
    void DDD3_防腐層只引用上游公布的格式_不會被擋() {
        sampleContextMap("contextmapok").check(Imported.fixture("contextmapok"));
    }
}
