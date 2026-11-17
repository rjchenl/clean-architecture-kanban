package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

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

    /**
     * CA-7：Interface Adapters 圈只能使用 JDK（含 java.sql／javax.sql）、Jackson 與內兩圈。
     * 走允許清單：任何其他函式庫或框架（Spring、Jakarta、資料庫驅動程式、Tomcat、ORM…）都會被擋下。
     */
    public static final ArchRule ADAPTERS_FREE_OF_FRAMEWORKS = classes()
            .that().resideInAPackage("..adapters..")
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    "java..", "javax.sql..", "tools.jackson..", "..entities..", "..usecases..", "..adapters..")
            .as("CA-7 Interface Adapters 圈只能使用 JDK、Jackson 與內兩圈");

    /** CA-7：Spring 只能出現在最外圈與 Main。 */
    public static final ArchRule SPRING_ANNOTATIONS_ONLY_IN_OUTER_RING = noClasses()
            .that().resideInAnyPackage("..entities..", "..usecases..", "..adapters..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .as("CA-7 內三圈不得出現 Spring 的任何型別與註解");

    /**
     * CA-12：各圈之間、各業務情境之間的相依圖沒有循環——以「業務情境.圈」為一塊，不列情境名稱。
     * 正式程式碼用 {@link #NO_PACKAGE_CYCLES}。
     */
    public static ArchRule noCyclesBetweenRings(String root) {
        return slices()
                .matching(root + ".(*).(*)..")
                .should().beFreeOfCycles()
                .as("CA-12 元件相依圖不得有循環");
    }

    public static final ArchRule NO_PACKAGE_CYCLES = noCyclesBetweenRings("kanban");

    /**
     * CA-12：同一圈裡的套件之間也沒有循環——每個套件各算一塊（例如兩個指令套件互相引用）。
     * 正式程式碼用 {@link #NO_CYCLES_BETWEEN_PACKAGES}。
     */
    public static ArchRule noCyclesBetweenPackages(String root) {
        return slices()
                .matching(root + ".(**)")
                .should().beFreeOfCycles()
                .as("CA-12 同一圈裡的套件之間不得有循環");
    }

    public static final ArchRule NO_CYCLES_BETWEEN_PACKAGES = noCyclesBetweenPackages("kanban");

    /** CA-10：最上層是業務情境，其下才分四圈。正式程式碼用 {@link #TOP_LEVEL_PACKAGES_SCREAM}。 */
    public static ArchRule topLevelPackagesScream(String root) {
        return classes()
                .that().resideInAPackage(root + "..")
                .should().resideInAnyPackage(
                        root + ".boardmanagement.entities..",
                        root + ".boardmanagement.usecases..",
                        root + ".boardmanagement.adapters..",
                        root + ".boardmanagement.frameworks..",
                        root + ".flowanalysis.entities..",
                        root + ".flowanalysis.usecases..",
                        root + ".flowanalysis.adapters..",
                        root + ".flowanalysis.frameworks..",
                        root + ".main..")
                .as("CA-10 最上層套件只能是業務情境（其下分四圈）與 main");
    }

    public static final ArchRule TOP_LEVEL_PACKAGES_SCREAM = topLevelPackagesScream("kanban");

    /**
     * CA-8、CA-11 DIP：只有 Main 知道具體實作。
     * (1) Use Case 的實作類別（impl、support 套件）只能被自己人與 Main 引用；
     * (2) Interface Adapters 圈的行為類別只能由 Main 建立（資料 record 不在此限）。
     */
    public static final ArchRule ONLY_MAIN_KNOWS_IMPLEMENTATIONS = classes()
            .that().resideInAnyPackage("..usecases..", "..adapters..", "..frameworks..")
            .should(new ArchCondition<>("不引用 Use Case 實作類別，也不自己建立 Adapters 圈的行為類別") {
                @Override
                public void check(JavaClass origin, ConditionEvents events) {
                    boolean originIsImplementation =
                            Shapes.inPackage(origin, "impl") || Shapes.inPackage(origin, "support");
                    for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                        JavaClass target = dependency.getTargetClass();
                        boolean targetIsUseCaseImplementation = Shapes.inPackage(target, "usecases")
                                && (Shapes.inPackage(target, "impl") || Shapes.inPackage(target, "support"));
                        if (targetIsUseCaseImplementation && !originIsImplementation) {
                            events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                        }
                    }
                    java.util.List<com.tngtech.archunit.core.domain.JavaCodeUnitAccess<?>> creations = new java.util.ArrayList<>();
                    creations.addAll(origin.getConstructorCallsFromSelf());
                    // 建構子參考（SomeAdapter::new）也是在建立物件
                    creations.addAll(origin.getConstructorReferencesFromSelf());
                    for (com.tngtech.archunit.core.domain.JavaCodeUnitAccess<?> call : creations) {
                        JavaClass target = call.getTargetOwner();
                        boolean createsAdapter = Shapes.inPackage(target, "adapters")
                                && Shapes.isBehaviourClass(target)
                                && !target.equals(origin)
                                // 子類別的建構子呼叫父類別的建構子是繼承，不是另外建立一個物件
                                && !origin.isAssignableTo(target.getName())
                                && !target.getEnclosingClass().map(origin::equals).orElse(false)
                                && !origin.getEnclosingClass().map(target::equals).orElse(false);
                        if (createsAdapter) {
                            events.add(SimpleConditionEvent.violated(call, call.getDescription()));
                        }
                    }
                }
            })
            .as("CA-8 只有 Main 可以引用 Use Case 實作類別、建立 Interface Adapters 圈的行為類別");

    /** CA-11 ISP：事件存取依使用者拆成兩個介面，各自只被指定的使用者引用。 */
    public static final ArchRule EVENT_ACCESS_IS_SEGREGATED = classes()
            .that().resideInAnyPackage("..usecases..", "..adapters..", "..frameworks..")
            .should(new ArchCondition<>("只有 Repository 的實作使用 EventStore、只有 Projection 使用 EventFeed") {
                @Override
                public void check(JavaClass origin, ConditionEvents events) {
                    for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                        JavaClass target = dependency.getTargetClass();
                        if (!Shapes.inPackage(target, "usecases") || target.equals(origin)) {
                            continue;
                        }
                        boolean inEventStorePackage = Shapes.inPackage(origin, "eventstore");
                        boolean inProjectionPackage = Shapes.inPackage(origin, "projection");
                        boolean wrongUserOfStore = target.getSimpleName().equals("EventStore") && !inEventStorePackage;
                        boolean wrongUserOfFeed = target.getSimpleName().equals("EventFeed")
                                && !inEventStorePackage && !inProjectionPackage;
                        if (wrongUserOfStore || wrongUserOfFeed) {
                            events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                        }
                    }
                }
            })
            .as("CA-11 ISP EventStore 只給 adapters.eventstore 用；EventFeed 只給 adapters.eventstore 與 adapters.projection 用");

    /**
     * CA-10、ES-1：Interface Adapters 圈的套件走允許清單。
     * 想新開一個套件放別的東西（例如另存看板現況的儲存），要先改這份清單。
     */
    public static final ArchRule ADAPTER_PACKAGES_ON_ALLOW_LIST = classes()
            .that().resideInAPackage("..adapters..")
            .should().resideInAnyPackage("..adapters.controllers", "..adapters.presenters", "..adapters.eventstore",
                    "..adapters.projection", "..adapters.system", "..adapters.boardactivity")
            .as("CA-10 Interface Adapters 圈只有 controllers、presenters、eventstore、projection、system，"
                    + "以及流程分析的防腐層 boardactivity 六個套件");
}
