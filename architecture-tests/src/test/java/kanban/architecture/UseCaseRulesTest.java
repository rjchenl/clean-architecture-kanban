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
    void CQRS4_UseCase套件裡只有邊界與RequestResponse() {
        UseCaseRules.USECASE_PACKAGES_HOLD_ONLY_BOUNDARY_TYPES.check(Imported.production());
    }

    @Test
    void CQRS4_指令UseCase只用寫入端Gateway() {
        UseCaseRules.COMMAND_USECASES_USE_ONLY_WRITE_GATEWAYS.check(Imported.production());
    }

    @Test
    void CA7_Adapters模組宣告只requires允許的模組() throws IOException {
        String moduleInfo = Files.readString(
                Path.of("..", "board-management", "interface-adapters", "src", "main", "java", "module-info.java"));

        assertThat(requiredModules(moduleInfo)).containsExactlyInAnyOrder(
                "kanban.boardmanagement.usecases", "java.sql", "tools.jackson.databind");
    }

    @Test
    void CA2_CA3_內兩圈的模組宣告不requires任何外部模組() throws IOException {
        String entities = Files.readString(
                Path.of("..", "board-management", "entities", "src", "main", "java", "module-info.java"));
        String useCases = Files.readString(
                Path.of("..", "board-management", "use-cases", "src", "main", "java", "module-info.java"));

        assertThat(requiredModules(entities)).isEmpty();
        assertThat(requiredModules(useCases)).containsExactly("kanban.boardmanagement.entities");
    }

    @Test
    void CA2_CA3_流程分析內兩圈的模組宣告不requires任何外部模組() throws IOException {
        String entities = Files.readString(
                Path.of("..", "flow-analysis", "entities", "src", "main", "java", "module-info.java"));
        String useCases = Files.readString(
                Path.of("..", "flow-analysis", "use-cases", "src", "main", "java", "module-info.java"));

        assertThat(requiredModules(entities)).isEmpty();
        assertThat(requiredModules(useCases)).containsExactly("kanban.flowanalysis.entities");
    }

    @Test
    void CA7_流程分析Adapters模組宣告只requires允許的模組_不認識看板管理() throws IOException {
        String moduleInfo = Files.readString(
                Path.of("..", "flow-analysis", "interface-adapters", "src", "main", "java", "module-info.java"));

        assertThat(requiredModules(moduleInfo)).containsExactlyInAnyOrder(
                "kanban.flowanalysis.usecases", "java.sql", "tools.jackson.databind");
    }

    @Test
    void CA16_DDD3_防腐層模組宣告只requires兩個情境的UseCases與JDBC() throws IOException {
        // 設計第 0 節 4a：防腐層是流程分析裡唯一 requires 看板管理的模組
        String moduleInfo = Files.readString(Path.of(
                "..", "flow-analysis", "board-activity-translation", "src", "main", "java", "module-info.java"));

        assertThat(requiredModules(moduleInfo)).containsExactlyInAnyOrder(
                "kanban.flowanalysis.usecases", "kanban.boardmanagement.usecases", "java.sql");
    }

    @Test
    void CA16_流程分析的模組宣告不公開impl與support套件() throws IOException {
        String moduleInfo = Files.readString(
                Path.of("..", "flow-analysis", "use-cases", "src", "main", "java", "module-info.java"));

        assertThat(exportedPackages(moduleInfo)).isNotEmpty().noneMatch(UseCaseRulesTest::isInternal);
    }

    private static List<String> requiredModules(String moduleInfo) {
        Matcher matcher = Pattern.compile("requires\\s+(?:transitive\\s+)?([\\w.]+)\\s*;").matcher(moduleInfo);
        return matcher.results().map(result -> result.group(1)).toList();
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
