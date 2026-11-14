package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.AdapterRules;
import org.junit.jupiter.api.Test;

/** 證明每條規則遇到違規樣本真的會失敗。 */
class AdapterRulesBiteTest {

    @Test
    void CA9_ViewModel帶著還沒整理的時間會被擋下() {
        assertBites(AdapterRules.VIEWMODELS_ARE_DISPLAY_READY, "rawviewmodel");
    }

    @Test
    void CA9_ViewModel帶著數字讓前端自己算會被擋下() {
        assertBites(AdapterRules.VIEWMODELS_ARE_DISPLAY_READY, "numericviewmodel");
    }

    @Test
    void CA11_DIP_Controller直接用Gateway會被擋下() {
        assertBites(AdapterRules.CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES, "controllerusesgateway");
    }

    @Test
    void CA8_Controller認識Presenter會被擋下() {
        assertBites(AdapterRules.CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES, "controllerknowspresenter");
    }
}
