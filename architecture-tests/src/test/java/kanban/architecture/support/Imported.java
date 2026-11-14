package kanban.architecture.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/** 架構測試共用：讀入正式程式碼或違規樣本。 */
public final class Imported {

    private Imported() {
    }

    private static final JavaClasses PRODUCTION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("kanban.boardmanagement", "kanban.main");

    /** 正式程式碼（不含測試、不含違規樣本）。 */
    public static JavaClasses production() {
        return PRODUCTION;
    }

    /** 某一條規則的違規樣本。 */
    public static JavaClasses fixture(String name) {
        JavaClasses classes = new ClassFileImporter().importPackages("kanban.fixtures." + name);
        assertThat(classes).as("違規樣本 kanban.fixtures.%s 不可以是空的", name).isNotEmpty();
        return classes;
    }

    /** 斷言規則遇到違規樣本會失敗——證明規則真的有在檢查。 */
    public static void assertBites(ArchRule rule, String fixtureName) {
        JavaClasses classes = fixture(fixtureName);
        assertThatThrownBy(() -> rule.check(classes))
                .as("規則「%s」應該擋下樣本 %s", rule.getDescription(), fixtureName)
                .isInstanceOf(AssertionError.class);
    }
}
