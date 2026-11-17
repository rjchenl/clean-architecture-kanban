package kanban.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.architecture.support.Glossary;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** DDD-1：領域模型的公開名稱與指令名稱都必須出現在 docs/glossary.md。 */
class GlossaryTest {

    @Test
    void 用語表讀得到名稱() {
        assertThat(Glossary.codeNames()).contains("Board", "WipLimit", "CardMoved", "MoveCard",
                "BoardFlow", "WorkItemMoved", "SetColumnStage");
    }

    @Test
    void 用語表讀得到聚合內部物件_DDD4規則靠它() {
        assertThat(Glossary.codeNamesOfKind("聚合內部物件")).containsExactlyInAnyOrder("Column", "Card",
                "FlowColumn", "WorkItem", "ColumnVisit", "FlowResult");
    }

    @Test
    void 領域模型與指令的名稱都在用語表裡() {
        assertThat(Glossary.missing(Imported.production()))
                .as("這些名稱還沒加進 docs/glossary.md（新名詞要先進用語表再寫程式）")
                .isEmpty();
    }

    @Test
    void 用語表依業務情境分節讀取() {
        assertThat(Glossary.codeNamesIn("boardmanagement")).contains("Board", "CardMoved", "BoardActivity")
                .doesNotContain("BoardFlow", "程式名稱");
        assertThat(Glossary.codeNamesIn("flowanalysis")).contains("BoardFlow", "WorkItemMoved", "RejectionReason")
                .doesNotContain("Board", "CardMoved", "程式名稱");
    }

    @Test
    void DDD1_每個情境的名稱都在自己那一節() {
        assertThat(Glossary.missingPerContext(Imported.production(), "kanban"))
                .as("這些名稱要加進 docs/glossary.md 裡自己業務情境的那一節（同一個詞在不同情境可以有不同意義，DDD-2）")
                .isEmpty();
    }

    @Test
    void 用語表只列在別的情境_被抓出來() {
        // 樣本：流程分析的 Entities 圈有一個 ColumnName，但 ColumnName 只列在看板管理那一節
        assertThat(Glossary.missingPerContext(Imported.fixture("glossarycontext"), "kanban.fixtures.glossarycontext"))
                .containsExactly("flowanalysis: ColumnName");
        // 對照：不分情境的舊檢查只要名稱出現在任何一節就放行，所以抓不到這個樣本
        assertThat(Glossary.missing(Imported.fixture("glossarycontext"))).isEmpty();
    }

    @Test
    void 不在用語表裡的領域類別會被抓出來() {
        assertThat(Glossary.missing(Imported.fixture("glossary")))
                .containsExactlyInAnyOrder("UnlistedThing", "UnlistedCommand");
    }
}
