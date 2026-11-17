package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaMethodReference;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** CQRS 的架構規則。規則編號對應專案 CLAUDE.md。 */
public final class CqrsRules {

    private CqrsRules() {
    }

    /** CQRS-1：指令與查詢是兩條分開的路，兩邊的套件互不引用。 */
    public static final ArchRule COMMANDS_AND_QUERIES_SEPARATE = noClasses()
            .that().resideInAPackage("..usecases.commands..")
            .should().dependOnClassesThat().resideInAPackage("..usecases.queries..")
            .as("CQRS-1 指令端不得引用查詢端");

    /** CQRS-2、CQRS-3：查詢端不載入聚合、不讀寫事件紀錄、不引用任何寫入介面。 */
    public static final ArchRule QUERIES_NEVER_TOUCH_WRITE_SIDE = noClasses()
            .that().resideInAPackage("..usecases.queries..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..entities..", "..usecases.commands..", "..usecases.eventfeed..")
            .as("CQRS-3 查詢端不得引用聚合、指令端或事件紀錄");

    /** CQRS-4：寫入端不用讀取模型做業務決定。 */
    public static final ArchRule COMMANDS_NEVER_READ_READ_MODEL = noClasses()
            .that().resideInAnyPackage("..entities..", "..usecases.commands..", "..adapters.eventstore..")
            .should().dependOnClassesThat().resideInAnyPackage("..usecases.queries..", "..adapters.projection..")
            .as("CQRS-4 寫入端（聚合、指令 Use Case、事件庫）不得引用讀取模型");

    /**
     * 讀取模型的儲存，兩個條件任一成立就算（第二階段補強：只會更嚴，不會更鬆）：
     * (1) 名稱：ReadModelStore 介面本身，或任何實作它的類別；
     * (2) 形狀：放在 adapters.projection、而且服務查詢端（是某個 usecases.queries 介面的子型別）的型別——
     *     換個名字的儲存也認得出來。
     */
    private static boolean isReadModelStore(JavaClass type) {
        boolean byName = type.getSimpleName().equals("ReadModelStore")
                || type.getAllRawInterfaces().stream().anyMatch(i -> i.getSimpleName().equals("ReadModelStore"));
        boolean byShape = Shapes.inPackage(type, "adapters") && Shapes.inPackage(type, "projection")
                && type.getAllRawInterfaces().stream().anyMatch(CqrsRules::isQueryInterface);
        return byName || byShape;
    }

    private static boolean isQueryInterface(JavaClass type) {
        return type.isInterface() && Shapes.inPackage(type, "usecases") && Shapes.inPackage(type, "queries");
    }

    /** 正式程式碼裡被認成讀取模型儲存的型別（給「每個儲存都被認到」的測試用）。 */
    public static Set<String> readModelStoresIn(JavaClasses classes) {
        Set<String> stores = new TreeSet<>();
        for (JavaClass type : classes) {
            if (isReadModelStore(type)) {
                stores.add(type.getName());
            }
        }
        return stores;
    }

    private static final Set<String> READ_MODEL_WRITES =
            Set.of("saveBoard", "saveCardHistory", "advanceCheckpoint", "reset", "inExclusiveTransaction");

    private static final Set<String> OBJECT_METHODS = Set.of("equals", "hashCode", "toString", "getClass");

    /**
     * 呼叫的是不是讀取模型儲存的寫入方法，兩個條件任一成立就算：
     * (1) 名稱在舊的名單裡；(2) 儲存自己宣告、但沒有任何查詢端介面宣告過的方法（查詢端介面宣告的才是「讀」）。
     */
    private static boolean isReadModelWrite(JavaClass owner, String name, List<JavaClass> parameterTypes) {
        if (!isReadModelStore(owner)) {
            return false;
        }
        if (READ_MODEL_WRITES.contains(name)) {
            return true;
        }
        if (OBJECT_METHODS.contains(name)) {
            return false;
        }
        List<String> parameters = parameterNames(parameterTypes);
        boolean declaredByQuerySide = owner.getAllRawInterfaces().stream()
                .filter(CqrsRules::isQueryInterface)
                .flatMap(query -> query.getMethods().stream())
                .anyMatch(read -> read.getName().equals(name)
                        && parameterNames(read.getRawParameterTypes()).equals(parameters));
        return !declaredByQuerySide;
    }

    private static List<String> parameterNames(List<JavaClass> types) {
        return types.stream().map(JavaClass::getName).toList();
    }

    /**
     * CQRS-5、CA-11 ISP：讀取模型只能由 Projection 更新。
     * (1) 讀取模型儲存的寫入方法只有名稱以 Projection 結尾的類別可以呼叫；
     * (2) 讀取模型的儲存只能被 adapters.projection 套件引用（Main 與最外圈不在檢查範圍）。
     * 「讀取模型的儲存」包含 ReadModelStore 介面與任何實作它的具體類別，直接拿具體類別也繞不過去。
     */
    public static final ArchRule ONLY_PROJECTION_WRITES_READ_MODEL = classes()
            .that().resideInAnyPackage("..entities..", "..usecases..", "..adapters..")
            .should(new ArchCondition<>("不寫入讀取模型（Projection 除外）") {
                @Override
                public void check(JavaClass origin, ConditionEvents events) {
                    if (isReadModelStore(origin)) {
                        return;
                    }
                    boolean isProjection = origin.getSimpleName().endsWith("Projection");
                    boolean inProjectionPackage = Shapes.inPackage(origin, "projection");
                    for (JavaMethodCall call : origin.getMethodCallsFromSelf()) {
                        if (!isProjection && isReadModelWrite(call.getTargetOwner(), call.getTarget().getName(),
                                call.getTarget().getRawParameterTypes())) {
                            events.add(SimpleConditionEvent.violated(call, call.getDescription()));
                        }
                    }
                    // 方法參考（store::append）也是在寫入
                    for (JavaMethodReference reference : origin.getMethodReferencesFromSelf()) {
                        if (!isProjection && isReadModelWrite(reference.getTargetOwner(),
                                reference.getTarget().getName(), reference.getTarget().getRawParameterTypes())) {
                            events.add(SimpleConditionEvent.violated(reference, reference.getDescription()));
                        }
                    }
                    for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                        boolean usesStore = isReadModelStore(dependency.getTargetClass());
                        if (usesStore && !inProjectionPackage) {
                            events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                        }
                    }
                }
            })
            .as("CQRS-5 讀取模型只能由 Projection 根據事件更新");
}
