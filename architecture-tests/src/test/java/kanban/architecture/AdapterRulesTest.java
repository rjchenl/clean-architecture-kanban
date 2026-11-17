package kanban.architecture;

import kanban.architecture.rules.AdapterRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** 對正式程式碼跑 Interface Adapters 圈畫面側的規則。 */
class AdapterRulesTest {

    @Test
    void CA9_ViewModel只含可直接顯示的資料() {
        AdapterRules.VIEWMODELS_ARE_DISPLAY_READY.check(Imported.production());
    }

    @Test
    void CA11_OCP_一個Presenter只實作一個Output() {
        AdapterRules.ONE_PRESENTER_PER_OUTPUT.check(Imported.production());
    }

    @Test
    void CA11_DIP_Controller只用InputBoundary() {
        AdapterRules.CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES.check(Imported.production());
    }

    @Test
    void DDD3_防腐層只翻譯() {
        AdapterRules.TRANSLATION_ONLY_TRANSLATES.check(Imported.production());
    }
}
