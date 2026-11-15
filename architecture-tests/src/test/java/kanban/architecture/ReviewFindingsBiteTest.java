package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.CleanArchitectureRules;
import kanban.architecture.rules.CqrsRules;
import kanban.architecture.rules.EntitiesRules;
import kanban.architecture.rules.EventSourcingRules;
import kanban.architecture.rules.UseCaseRules;
import org.junit.jupiter.api.Test;

/**
 * 2026-11-14 獨立審查找到的繞過寫法。每一種都做成違規樣本，證明補強後的規則擋得住。
 */
class ReviewFindingsBiteTest {

    @Test
    void CQRS4_在指令UseCase套件裡偷放Gateway會被擋下() {
        assertBites(UseCaseRules.USECASE_PACKAGES_HOLD_ONLY_BOUNDARY_TYPES, "wipcounter");
    }

    @Test
    void CQRS4_指令UseCase多拿一個協作者會被擋下() {
        assertBites(UseCaseRules.COMMAND_USECASES_USE_ONLY_WRITE_GATEWAYS, "commandfield");
    }

    @Test
    void ES1_Adapters圈新開不在清單上的套件會被擋下() {
        assertBites(CleanArchitectureRules.ADAPTER_PACKAGES_ON_ALLOW_LIST, "adapterpackage");
    }

    @Test
    void ES1_事件庫與讀取模型以外的類別碰資料庫會被擋下() {
        assertBites(EventSourcingRules.JDBC_ONLY_IN_POSTGRES_STORES, "jdbcelsewhere");
    }

    @Test
    void CQRS5_直接拿具體實作改讀取模型會被擋下() {
        assertBites(CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL, "readmodelimpl");
    }

    @Test
    void CA7_Adapters圈用允許清單以外的函式庫會被擋下() {
        assertBites(CleanArchitectureRules.ADAPTERS_FREE_OF_FRAMEWORKS, "adapterslibrary");
    }

    @Test
    void ES3_用可變集合在指令方法裡直接改狀態會被擋下() {
        assertBites(EntitiesRules.STATE_CHANGES_ONLY_IN_APPLY, "mutablestate");
    }

    @Test
    void ES3_套用方法間接丟例外會被擋下() {
        assertBites(EntitiesRules.APPLY_METHODS_DO_NOT_REJECT, "applyindirect");
    }

    @Test
    void CA8_用建構子參考建立轉接程式會被擋下() {
        assertBites(CleanArchitectureRules.ONLY_MAIN_KNOWS_IMPLEMENTATIONS, "ctorreference");
    }

    @Test
    void CA4_impl套件裡沒有實作Input的UseCase會被擋下() {
        assertBites(UseCaseRules.NO_UNNECESSARY_PUBLIC, "sneakyusecase");
    }
}
