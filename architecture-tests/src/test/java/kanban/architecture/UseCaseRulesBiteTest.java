package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.UseCaseRules;
import org.junit.jupiter.api.Test;

/** 證明每條 Use Cases 圈規則遇到違規樣本真的會失敗。 */
class UseCaseRulesBiteTest {

    @Test
    void CA4_只有Input沒有Output與實作會被擋下() {
        assertBites(UseCaseRules.EVERY_USECASE_HAS_BOTH_BOUNDARIES, "noboundary");
    }

    @Test
    void CA11_ISP_兩個方法的邊界介面會被擋下() {
        assertBites(UseCaseRules.INPUT_BOUNDARY_HAS_ONE_METHOD, "fatboundary");
    }

    @Test
    void CA5_把領域物件放進Request會被擋下() {
        assertBites(UseCaseRules.BOUNDARY_TYPES_ARE_SIMPLE, "entityinrequest");
    }

    @Test
    void CA5_邊界方法直接收領域物件會被擋下() {
        assertBites(UseCaseRules.BOUNDARY_TYPES_ARE_SIMPLE, "entityinboundary");
    }

    @Test
    void DDD5_一個指令持有兩個Repository會被擋下() {
        assertBites(UseCaseRules.ONE_AGGREGATE_PER_COMMAND, "twoaggregates");
    }

    @Test
    void CQRS2_指令的Output帶畫面資料會被擋下() {
        assertBites(UseCaseRules.COMMAND_RESPONSE_CARRIES_NO_VIEW_DATA, "viewdata");
    }

    @Test
    void CA16_impl套件放別的類別會被擋下() {
        assertBites(UseCaseRules.NO_UNNECESSARY_PUBLIC, "strayimpl");
    }

    @Test
    void CA10_以service命名的套件會被擋下() {
        assertBites(UseCaseRules.USECASE_PACKAGES_ON_ALLOW_LIST, "servicepackage");
    }
}
