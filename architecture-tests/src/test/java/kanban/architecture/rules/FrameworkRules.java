package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/** Frameworks & Drivers 圈的架構規則。 */
public final class FrameworkRules {

    private FrameworkRules() {
    }

    /** CA-7、CA-9：HTTP 薄殼只轉手——不認識 Use Case、聚合、Controller、Gateway 轉接程式，只看得到 ViewModel。 */
    public static final ArchRule ENDPOINTS_ONLY_DELEGATE = noClasses()
            .that().resideInAPackage("..frameworks.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..usecases..", "..entities..", "..adapters.controllers..", "..adapters.eventstore..",
                    "..adapters.projection..", "..adapters.system..")
            .as("CA-7 HTTP 薄殼只能使用 ViewModel 與自己定義的介面，不得認識內圈的其他東西");
}
