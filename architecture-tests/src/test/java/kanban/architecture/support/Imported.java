package kanban.architecture.support;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.lang.ArchRule;

/** 架構測試共用：讀入正式程式碼或違規樣本。 */
public final class Imported {

    private Imported() {
    }

    /** 測試程式的位置：Maven 同一次建置給 test-classes 資料夾，單獨建置時給 -tests.jar。 */
    private static boolean isTestOutput(Location location) {
        return location.contains("-tests.jar") || location.contains("/test-classes/");
    }

    /**
     * 正式程式碼：kanban 底下全部，不列業務情境名稱——新業務情境的模組只要在本模組的 classpath 上
     * （目前經由 kanban-main 帶進來），不必改這裡就會被每一條規則檢查到。
     * entities 的 test-jar 也在 classpath 上（給 ES-5 規則用），正式程式碼的檢查要排除它。
     */
    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(location -> !isTestOutput(location))
            .importPackages("kanban");

    /** 正式程式碼（不含測試、不含違規樣本）。 */
    public static JavaClasses production() {
        return PRODUCTION;
    }

    /**
     * Entities 圈的全部測試程式（來自各業務情境 entities 模組的 test-jar），用來檢查聚合測試的寫法。
     * 以位置與套件挑選、不看檔名：Given／When／Then 工具、巢狀類別、輔助類別都算；本模組的違規樣本不算。
     */
    public static JavaClasses entitiesTests() {
        return new ClassFileImporter()
                .withImportOption(Imported::isTestOutput)
                .importPackages("kanban")
                .that(resideInAPackage("..entities..").and(DescribedPredicate.not(resideInAPackage("kanban.fixtures.."))));
    }

    /** 某一條規則的違規樣本。 */
    public static JavaClasses fixture(String name) {
        JavaClasses classes = new ClassFileImporter().importPackages("kanban.fixtures." + name);
        assertThat(classes).as("違規樣本 kanban.fixtures.%s 不可以是空的", name).isNotEmpty();
        return classes;
    }

    /**
     * 斷言規則遇到違規樣本會失敗——證明規則真的有在檢查。
     * 必須是「有違規」的失敗：規則的 that() 一個類別都沒選到時，ArchUnit 也會丟 AssertionError
     * （failed to check any classes），那代表規則根本沒看到樣本，不算擋下。
     */
    public static void assertBites(ArchRule rule, String fixtureName) {
        JavaClasses classes = fixture(fixtureName);
        assertThatThrownBy(() -> rule.check(classes))
                .as("規則「%s」應該擋下樣本 %s", rule.getDescription(), fixtureName)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("was violated");
    }
}
