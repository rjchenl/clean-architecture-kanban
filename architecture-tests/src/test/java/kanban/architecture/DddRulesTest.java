package kanban.architecture;

import kanban.architecture.rules.DddRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** 對正式程式碼跑 DDD 規則。 */
class DddRulesTest {

    @Test
    void DDD8_Gateway介面在內圈_實作在外圈() {
        DddRules.REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE.check(Imported.production());
    }

    @Test
    void DDD2_業務情境之間不共用領域類別() {
        DddRules.CONTEXTS_DO_NOT_SHARE_DOMAIN.check(Imported.production());
    }
}
