package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

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

    /**
     * CA-7（2026-11-17 獨立審查 I-1）：整個最外圈都只能轉手，不只 web 與 scheduling。
     * 在最外圈另開一個套件放輔助類別、讓端點或排程透過它碰內圈，也會被擋下。Main 不在最外圈的套件裡，不受這條限制。
     */
    public static final ArchRule FRAMEWORK_RING_ONLY_DELEGATES = noClasses()
            .that().resideInAPackage("..frameworks..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..usecases..", "..entities..", "..adapters.controllers..", "..adapters.eventstore..",
                    "..adapters.projection..", "..adapters.system..", "..adapters.boardactivity..")
            .as("CA-7 最外圈的每個套件都只能使用 ViewModel 與自己定義的介面，不得認識內圈的其他東西");

    private static final String SCHEDULED = "org.springframework.scheduling.annotation.Scheduled";
    private static final String SCHEDULES = "org.springframework.scheduling.annotation.Schedules";
    private static final String CONTROLLER = "org.springframework.stereotype.Controller";
    private static final String CONTROLLER_ADVICE = "org.springframework.web.bind.annotation.ControllerAdvice";

    private static boolean isHttpEntryPoint(JavaClass type) {
        return type.isAnnotatedWith(CONTROLLER) || type.isMetaAnnotatedWith(CONTROLLER)
                || type.isAnnotatedWith(CONTROLLER_ADVICE) || type.isMetaAnnotatedWith(CONTROLLER_ADVICE);
    }

    private static boolean isSchedule(JavaClass type) {
        return type.getMethods().stream()
                .anyMatch(method -> method.isAnnotatedWith(SCHEDULED) || method.isAnnotatedWith(SCHEDULES));
    }

    /**
     * CA-7（2026-11-17 獨立審查 I-1）：Spring 的進入點放在規定的地方——HTTP 端點（含錯誤處理）在 frameworks.web、
     * 排程在 frameworks.scheduling。放到別的套件或 Main，就躲開了「HTTP 薄殼只轉手」「背景排程只轉手」。
     */
    public static final ArchRule ENTRY_POINTS_IN_THEIR_PACKAGES = classes()
            .that(new DescribedPredicate<JavaClass>("是 HTTP 端點或排程") {
                @Override
                public boolean test(JavaClass type) {
                    return isHttpEntryPoint(type) || isSchedule(type);
                }
            })
            .should(new ArchCondition<>("HTTP 端點在 frameworks.web、排程在 frameworks.scheduling") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    if (isHttpEntryPoint(type) && !type.getPackageName().contains(".frameworks.web")) {
                        events.add(SimpleConditionEvent.violated(type,
                                type.getName() + " 是 HTTP 端點，只能放在 frameworks.web"));
                    }
                    if (isSchedule(type) && !type.getPackageName().contains(".frameworks.scheduling")) {
                        events.add(SimpleConditionEvent.violated(type,
                                type.getName() + " 有排程，只能放在 frameworks.scheduling"));
                    }
                }
            })
            .as("CA-7 HTTP 端點在 frameworks.web、排程在 frameworks.scheduling");
}
