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
    void CA11_DIP_Controller只用InputBoundary() {
        AdapterRules.CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES.check(Imported.production());
    }
}
