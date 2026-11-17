package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

/** Frameworks & Drivers 圈的架構規則。 */
public final class FrameworkRules {

    private FrameworkRules() {
    }

    /**
     * CA-7、CA-9：HTTP 薄殼只轉手——不認識 Use Case、聚合、Controller、Gateway 轉接程式與防腐層，只看得到 ViewModel。
     * 防腐層（adapters.boardactivity）是第二階段加的：端點不得自己叫同步。
     */
    public static final ArchRule ENDPOINTS_ONLY_DELEGATE = noClasses()
            .that().resideInAPackage("..frameworks.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..usecases..", "..entities..", "..adapters.controllers..", "..adapters.eventstore..",
                    "..adapters.projection..", "..adapters.system..", "..adapters.boardactivity..")
            .as("CA-7 HTTP 薄殼只能使用 ViewModel 與自己定義的介面，不得認識內圈的其他東西");

    /**
     * CA-7、CA-9（規則目錄 B4）：背景排程只決定「什麼時候做」——和 HTTP 薄殼一樣不認識 Use Case、聚合、Controller、
     * Gateway 轉接程式與防腐層，只透過自己這一圈定義的介面轉手，看得到的只有 ViewModel。
     */
    public static final ArchRule SCHEDULES_ONLY_DELEGATE = noClasses()
            .that().resideInAPackage("..frameworks.scheduling..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..usecases..", "..entities..", "..adapters.controllers..", "..adapters.eventstore..",
                    "..adapters.projection..", "..adapters.system..", "..adapters.boardactivity..")
            .as("CA-7 背景排程只能使用 ViewModel 與自己定義的介面，不得認識內圈的其他東西");
}
