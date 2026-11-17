package kanban.architecture;

import static kanban.architecture.support.Imported.assertBites;

import kanban.architecture.rules.AdapterRules;
import kanban.architecture.rules.CleanArchitectureRules;
import kanban.architecture.rules.CqrsRules;
import kanban.architecture.rules.EventSourcingRules;
import kanban.architecture.rules.FrameworkRules;
import org.junit.jupiter.api.Test;

/**
 * 2026-11-17 第二階段獨立審查找到的繞過寫法，以及沒有專屬違規樣本的子條件。
 * 每一種都做成違規樣本：只違反它要證明的那一條，拿掉那一條檢查時這裡就會失敗。
 */
class PhaseTwoReviewFindingsBiteTest {

    @Test
    void CQRS5_讀取模型的儲存換到projection以外的套件會被擋下() {
        assertBites(CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL, "readmodeloutside");
    }

    @Test
    void CQRS5_不是Projection卻用方法參考清空讀取模型會被擋下() {
        assertBites(CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL, "readmodelmethodref");
    }

    @Test
    void CQRS5_不是Projection卻呼叫讀取模型儲存自己的寫入方法會被擋下() {
        assertBites(CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL, "readmodelshapewrite");
    }

    @Test
    void DDD3_防腐層兼任別的角色會被擋下() {
        assertBites(AdapterRules.TRANSLATION_ONLY_TRANSLATES, "translationrole");
    }

    @Test
    void ES1_取了允許清單上的簡名就在別的套件碰資料庫會被擋下() {
        assertBites(EventSourcingRules.JDBC_ONLY_IN_POSTGRES_STORES, "jdbcsamename");
    }

    @Test
    void CA7_最外圈另開套件讓端點透過它碰內圈會被擋下() {
        assertBites(FrameworkRules.FRAMEWORK_RING_ONLY_DELEGATES, "frameworksdetour");
    }

    @Test
    void CA7_排程與HTTP端點放在規定以外的套件會被擋下() {
        assertBites(FrameworkRules.ENTRY_POINTS_IN_THEIR_PACKAGES, "entrypointelsewhere");
    }

    @Test
    void CA8_在main模組以外宣告Main的套件會被擋下() {
        assertBites(CleanArchitectureRules.mainPackageComesOnlyFromMainModule("kanban.fixtures.mainspoof.main"),
                "mainspoof");
    }
}
