package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.CleanArchitectureRules;
import org.junit.jupiter.api.Test;

/** 證明每條 Clean Architecture 規則遇到違規樣本真的會失敗。 */
class CleanArchitectureRulesBiteTest {

    @Test
    void CA1_內圈引用外圈會被擋下() {
        assertBites(CleanArchitectureRules.RINGS_DEPEND_INWARD, "rings");
    }

    @Test
    void CA2_Entities圈用到函式庫會被擋下() {
        assertBites(CleanArchitectureRules.ENTITIES_USE_ONLY_JDK, "entitiesjdk");
    }

    @Test
    void CA3_UseCases圈用到函式庫會被擋下() {
        assertBites(CleanArchitectureRules.USECASES_USE_ONLY_JDK_AND_ENTITIES, "usecasesjdk");
    }

    @Test
    void CA7_Adapters圈用到Spring會被擋下() {
        assertBites(CleanArchitectureRules.ADAPTERS_FREE_OF_FRAMEWORKS, "adaptersframeworks");
    }

    @Test
    void CA7_內圈掛Spring註解會被擋下() {
        assertBites(CleanArchitectureRules.SPRING_ANNOTATIONS_ONLY_IN_OUTER_RING, "springinside");
    }

    @Test
    void CA12_循環相依會被擋下() {
        assertBites(CleanArchitectureRules.NO_PACKAGE_CYCLES, "cycles");
    }

    @Test
    void CA10_以controller當最上層套件會被擋下() {
        assertBites(CleanArchitectureRules.topLevelPackagesScream("kanban.fixtures.scream"), "scream");
    }
}
