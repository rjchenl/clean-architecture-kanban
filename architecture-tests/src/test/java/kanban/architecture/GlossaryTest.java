package kanban.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.architecture.support.Glossary;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** DDD-1：領域模型的公開名稱與指令名稱都必須出現在 docs/glossary.md。 */
class GlossaryTest {

    @Test
    void 用語表讀得到名稱() {
        assertThat(Glossary.codeNames()).contains("Board", "WipLimit", "CardMoved", "MoveCard");
    }

    @Test
    void 用語表讀得到聚合內部物件_DDD4規則靠它() {
        assertThat(Glossary.codeNamesOfKind("聚合內部物件")).containsExactlyInAnyOrder("Column", "Card");
    }

    @Test
    void 領域模型與指令的名稱都在用語表裡() {
        assertThat(Glossary.missing(Imported.production()))
                .as("這些名稱還沒加進 docs/glossary.md（新名詞要先進用語表再寫程式）")
                .isEmpty();
    }

    @Test
    void 不在用語表裡的領域類別會被抓出來() {
        assertThat(Glossary.missing(Imported.fixture("glossary")))
                .containsExactlyInAnyOrder("UnlistedThing", "UnlistedCommand");
    }
}
