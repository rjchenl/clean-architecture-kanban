package kanban.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import kanban.architecture.rules.UseCaseRules;
import kanban.architecture.support.Imported;
import org.junit.jupiter.api.Test;

/** 對正式程式碼跑 Use Cases 圈規則。 */
class UseCaseRulesTest {

    @Test
    void CA4_每個UseCase都有兩個邊界與實作() {
        UseCaseRules.EVERY_USECASE_HAS_BOTH_BOUNDARIES.check(Imported.production());
    }

    @Test
    void CA11_ISP_邊界介面只有一個方法() {
        UseCaseRules.INPUT_BOUNDARY_HAS_ONE_METHOD.check(Imported.production());
    }

    @Test
    void CA5_跨越邊界的資料是簡單資料() {
        UseCaseRules.BOUNDARY_TYPES_ARE_SIMPLE.check(Imported.production());
    }

    @Test
    void DDD5_一個指令只改一個聚合() {
        UseCaseRules.ONE_AGGREGATE_PER_COMMAND.check(Imported.production());
    }

    @Test
    void CQRS2_指令不帶回畫面資料() {
        UseCaseRules.COMMAND_RESPONSE_CARRIES_NO_VIEW_DATA.check(Imported.production());
    }

    @Test
    void CA16_impl套件只有UseCase實作() {
        UseCaseRules.NO_UNNECESSARY_PUBLIC.check(Imported.production());
    }

    @Test
    void CA10_UseCases圈的套件在允許清單內() {
        UseCaseRules.USECASE_PACKAGES_ON_ALLOW_LIST.check(Imported.production());
    }

    @Test
    void CA16_模組宣告不公開impl與support套件() throws IOException {
        String moduleInfo = Files.readString(
                Path.of("..", "board-management", "use-cases", "src", "main", "java", "module-info.java"));

        assertThat(exportedPackages(moduleInfo)).isNotEmpty().noneMatch(UseCaseRulesTest::isInternal);
    }

    @Test
    void CA16_公開impl套件的模組宣告會被抓出來() {
        String leaking = "module m { exports a.usecases.commands.movecard; exports a.usecases.commands.movecard.impl; }";

        assertThat(exportedPackages(leaking)).anyMatch(UseCaseRulesTest::isInternal);
    }

    private static boolean isInternal(String packageName) {
        return packageName.endsWith(".impl") || packageName.endsWith(".support");
    }

    private static List<String> exportedPackages(String moduleInfo) {
        Matcher matcher = Pattern.compile("exports\\s+([\\w.]+)\\s*;").matcher(moduleInfo);
        return matcher.results().map(result -> result.group(1)).toList();
    }
}
