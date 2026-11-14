package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.lang.ArchRule;

/** Clean Architecture 的架構規則。規則編號對應專案 CLAUDE.md。 */
public final class CleanArchitectureRules {

    private CleanArchitectureRules() {
    }

    /** CA-1：原始碼相依只能由外圈指向內圈。 */
    public static final ArchRule RINGS_DEPEND_INWARD = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Entities").definedBy("..entities..")
            .layer("UseCases").definedBy("..usecases..")
            .layer("Adapters").definedBy("..adapters..")
            .layer("Frameworks").definedBy("..frameworks..")
            .layer("Main").definedBy("..main..")
            .whereLayer("Main").mayNotBeAccessedByAnyLayer()
            .whereLayer("Frameworks").mayOnlyBeAccessedByLayers("Main")
            .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Frameworks", "Main")
            .whereLayer("UseCases").mayOnlyBeAccessedByLayers("Adapters", "Frameworks", "Main")
            .whereLayer("Entities").mayOnlyBeAccessedByLayers("UseCases", "Adapters", "Frameworks", "Main")
            .as("CA-1 相依只能由外圈指向內圈");

    /** CA-2、CA-7：Entities 圈是純 Java。 */
    public static final ArchRule ENTITIES_USE_ONLY_JDK = classes()
            .that().resideInAPackage("..entities..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..entities..")
            .as("CA-2 Entities 圈只能使用 JDK 與自己");

    /** CA-3、CA-7：Use Cases 圈只相依 Entities 圈。 */
    public static final ArchRule USECASES_USE_ONLY_JDK_AND_ENTITIES = classes()
            .that().resideInAPackage("..usecases..")
            .should().onlyDependOnClassesThat().resideInAnyPackage("java..", "..entities..", "..usecases..")
            .as("CA-3 Use Cases 圈只能使用 JDK、Entities 圈與自己");

    /** CA-7：Interface Adapters 圈不得使用框架與資料庫驅動程式。 */
    public static final ArchRule ADAPTERS_FREE_OF_FRAMEWORKS = noClasses()
            .that().resideInAPackage("..adapters..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "org.postgresql..", "com.zaxxer..", "org.flywaydb..")
            .as("CA-7 Interface Adapters 圈不得使用 Spring、Jakarta、資料庫驅動程式、連線池、Flyway");

    /** CA-7：Spring 只能出現在最外圈與 Main。 */
    public static final ArchRule SPRING_ANNOTATIONS_ONLY_IN_OUTER_RING = noClasses()
            .that().resideInAnyPackage("..entities..", "..usecases..", "..adapters..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .as("CA-7 內三圈不得出現 Spring 的任何型別與註解");

    /** CA-12：各圈之間、各業務情境之間的相依圖沒有循環。 */
    public static final ArchRule NO_PACKAGE_CYCLES = slices()
            .matching("..boardmanagement.(*)..")
            .should().beFreeOfCycles()
            .as("CA-12 元件相依圖不得有循環");

    /** CA-10：最上層是業務情境，其下才分四圈。正式程式碼用 {@link #TOP_LEVEL_PACKAGES_SCREAM}。 */
    public static ArchRule topLevelPackagesScream(String root) {
        return classes()
                .that().resideInAPackage(root + "..")
                .should().resideInAnyPackage(
                        root + ".boardmanagement.entities..",
                        root + ".boardmanagement.usecases..",
                        root + ".boardmanagement.adapters..",
                        root + ".boardmanagement.frameworks..",
                        root + ".main..")
                .as("CA-10 最上層套件只能是業務情境（其下分四圈）與 main");
    }

    public static final ArchRule TOP_LEVEL_PACKAGES_SCREAM = topLevelPackagesScream("kanban");
}
