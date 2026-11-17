package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.FrameworkRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

class FrameworkRulesTest {

    @Test
    void CA7_HTTP薄殼只轉手() {
        FrameworkRules.ENDPOINTS_ONLY_DELEGATE.check(Imported.production());
    }

    @Test
    void CA7_端點直接呼叫UseCase會被擋下() {
        assertBites(FrameworkRules.ENDPOINTS_ONLY_DELEGATE, "endpointcallsusecase");
    }

    @Test
    void CA7_端點直接用Repository會被擋下() {
        assertBites(FrameworkRules.ENDPOINTS_ONLY_DELEGATE, "endpointusesrepository");
    }

    @Test
    void CA7_端點直接叫防腐層會被擋下() {
        assertBites(FrameworkRules.ENDPOINTS_ONLY_DELEGATE, "endpointusestranslation");
    }

    @Test
    void CA7_背景排程只轉手() {
        FrameworkRules.SCHEDULES_ONLY_DELEGATE.check(Imported.production());
    }

    @Test
    void CA7_背景排程直接呼叫UseCase會被擋下() {
        assertBites(FrameworkRules.SCHEDULES_ONLY_DELEGATE, "schedulesmart");
    }

    @Test
    void CA7_最外圈每個套件都只轉手() {
        FrameworkRules.FRAMEWORK_RING_ONLY_DELEGATES.check(Imported.production());
    }

    @Test
    void CA7_HTTP端點與排程在規定的套件() {
        FrameworkRules.ENTRY_POINTS_IN_THEIR_PACKAGES.check(Imported.production());
    }
}
