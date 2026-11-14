package kanban.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.architecture.rules.CleanArchitectureRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** 對正式程式碼跑 Clean Architecture 規則。 */
class CleanArchitectureRulesTest {

    @Test
    void 讀得到各圈的正式類別() {
        assertThat(Imported.production().stream().map(c -> c.getPackageName()))
                .anyMatch(p -> p.startsWith("kanban.boardmanagement.entities"))
                .anyMatch(p -> p.startsWith("kanban.boardmanagement.usecases"))
                .anyMatch(p -> p.startsWith("kanban.boardmanagement.adapters"))
                .anyMatch(p -> p.startsWith("kanban.main"));
    }

    @Test
    void CA1_相依只能由外圈指向內圈() {
        CleanArchitectureRules.RINGS_DEPEND_INWARD.check(Imported.production());
    }

    @Test
    void CA2_Entities圈只用JDK() {
        CleanArchitectureRules.ENTITIES_USE_ONLY_JDK.check(Imported.production());
    }

    @Test
    void CA3_UseCases圈只用JDK與Entities() {
        CleanArchitectureRules.USECASES_USE_ONLY_JDK_AND_ENTITIES.check(Imported.production());
    }

    @Test
    void CA7_Adapters圈不用框架() {
        CleanArchitectureRules.ADAPTERS_FREE_OF_FRAMEWORKS.check(Imported.production());
    }

    @Test
    void CA7_內三圈沒有Spring() {
        CleanArchitectureRules.SPRING_ANNOTATIONS_ONLY_IN_OUTER_RING.check(Imported.production());
    }

    @Test
    void CA12_沒有循環相依() {
        CleanArchitectureRules.NO_PACKAGE_CYCLES.check(Imported.production());
    }

    @Test
    void CA8_只有Main知道具體實作() {
        CleanArchitectureRules.ONLY_MAIN_KNOWS_IMPLEMENTATIONS.check(Imported.production());
    }

    @Test
    void CA11_ISP_事件存取依使用者拆開() {
        CleanArchitectureRules.EVENT_ACCESS_IS_SEGREGATED.check(Imported.production());
    }

    @Test
    void CA10_Adapters圈的套件在允許清單內() {
        CleanArchitectureRules.ADAPTER_PACKAGES_ON_ALLOW_LIST.check(Imported.production());
    }

    @Test
    void CA10_最上層套件是業務情境() {
        CleanArchitectureRules.TOP_LEVEL_PACKAGES_SCREAM.check(Imported.production());
    }
}
