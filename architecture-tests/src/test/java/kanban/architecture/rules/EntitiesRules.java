package kanban.architecture.rules;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.time.Clock;
import java.util.Random;
import java.util.UUID;

/** Entities 圈（領域模型）的架構規則。規則編號對應專案 CLAUDE.md。 */
public final class EntitiesRules {

    private EntitiesRules() {
    }

    /** DDD-6、DDD-7：值物件與領域事件不可變——一律是 record。 */
    public static final ArchRule VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS = classes()
            .that().resideInAPackage("..entities..")
            .and().arePublic()
            .and().areNotInterfaces()
            .and().areNotEnums()
            .and().doNotHaveSimpleName("Board")
            .and().doNotHaveSimpleName("BusinessRuleViolation")
            .should().beRecords()
            .as("DDD-6／DDD-7 值物件與領域事件必須是 record（不可變、以值比較）");

    /** ES-4：領域模型不讀系統時間、不產生亂數；時間與識別碼由外部傳入。 */
    public static final ArchRule NO_CLOCK_OR_RANDOM_IN_ENTITIES = noClasses()
            .that().resideInAPackage("..entities..")
            .should().dependOnClassesThat().belongToAnyOf(Clock.class, Random.class)
            .orShould().dependOnClassesThat().resideInAPackage("java.util.concurrent..")
            .orShould().callMethodWhere(target(name("now")).and(target(owner(resideInAPackage("java.time..")))))
            .orShould().callMethod(System.class, "currentTimeMillis")
            .orShould().callMethod(System.class, "nanoTime")
            .orShould().callMethod(UUID.class, "randomUUID")
            .orShould().callMethod(Math.class, "random")
            .as("ES-4 Entities 圈不得讀取系統時間或產生亂數");

    // ------------------------------------------------------------------ 辨認用的述詞

    /** 聚合根：Entities 圈裡公開、不是 record／enum／介面／例外的類別（本專案是 Board）。 */
    private static final DescribedPredicate<JavaClass> AGGREGATE_ROOT = new DescribedPredicate<>("是聚合根") {
        @Override
        public boolean test(JavaClass type) {
            return (type.getPackageName() + ".").contains(".entities.")
                    && type.isTopLevelClass()
                    && type.getModifiers().contains(JavaModifier.PUBLIC)
                    && !type.isRecord() && !type.isEnum() && !type.isInterface()
                    && !type.isAssignableTo(Throwable.class);
        }
    };

    /** 領域事件：實作了名稱以 Event 結尾的介面。 */
    private static final DescribedPredicate<JavaClass> DOMAIN_EVENT = new DescribedPredicate<>("是領域事件") {
        @Override
        public boolean test(JavaClass type) {
            return !type.isInterface()
                    && type.getAllRawInterfaces().stream().anyMatch(i -> i.getSimpleName().endsWith("Event"));
        }
    };

    /** 套用事件的方法：名稱是 apply 或以 on 開頭（含它們內部的 lambda）。 */
    private static boolean isApplyMethod(JavaCodeUnit unit) {
        String name = unit.getName();
        if (name.startsWith("lambda$")) {
            name = name.substring("lambda$".length());
        }
        return name.equals("apply") || name.matches("on[A-Z].*") || name.matches("(apply|on[A-Z][^$]*)\\$\\d+");
    }

    // ------------------------------------------------------------------ 規則

    /** DDD-7：領域事件用過去式命名。 */
    public static final ArchRule EVENTS_NAMED_IN_PAST_TENSE = classes()
            .that(DOMAIN_EVENT)
            .should().haveSimpleNameEndingWith("Created")
            .orShould().haveSimpleNameEndingWith("Renamed")
            .orShould().haveSimpleNameEndingWith("Archived")
            .orShould().haveSimpleNameEndingWith("Added")
            .orShould().haveSimpleNameEndingWith("Moved")
            .orShould().haveSimpleNameEndingWith("Set")
            .orShould().haveSimpleNameEndingWith("Removed")
            .orShould().haveSimpleNameEndingWith("Changed")
            .orShould().haveSimpleNameEndingWith("Repositioned")
            .as("DDD-7 領域事件必須用過去式命名");

    /** DDD-4：聚合沒有公開 setter，公開方法也不把內部物件或可變集合交出去。 */
    public static final ArchRule AGGREGATE_HAS_NO_SETTERS = classes()
            .that(AGGREGATE_ROOT)
            .should(new ArchCondition<>("沒有公開 setter，也不外露內部物件") {
                @Override
                public void check(JavaClass aggregate, ConditionEvents events) {
                    for (JavaMethod method : aggregate.getMethods()) {
                        if (!method.getModifiers().contains(JavaModifier.PUBLIC)) {
                            continue;
                        }
                        if (method.getName().matches("set[A-Z].*") && method.getRawParameterTypes().size() == 1) {
                            events.add(SimpleConditionEvent.violated(method,
                                    method.getFullName() + " 是公開 setter"));
                        }
                        JavaClass returned = method.getRawReturnType();
                        boolean internal = (returned.getPackageName() + ".").contains(".entities.")
                                && !returned.getModifiers().contains(JavaModifier.PUBLIC);
                        boolean mutableCollection = !returned.isInterface()
                                && (returned.isAssignableTo(Collection.class) || returned.isAssignableTo(Map.class));
                        if (internal || mutableCollection) {
                            events.add(SimpleConditionEvent.violated(method,
                                    method.getFullName() + " 把內部物件或可變集合交給外部"));
                        }
                    }
                }
            })
            .as("DDD-4 聚合沒有公開 setter，也不外露內部物件");

    /** DDD-4、CA-16：聚合內部的實體（欄位、卡片）不是 public。 */
    public static final ArchRule AGGREGATE_INTERNALS_NOT_PUBLIC = classes()
            .that().resideInAPackage("..entities..")
            .and().haveSimpleName("Column").or().haveSimpleName("Card")
            .should().notBePublic()
            .as("DDD-4 聚合內部的實體不得是 public");

    /** 從套用方法出發，沿著「呼叫聚合自己的方法」一路找下去，回傳全部會被執行到的方法。 */
    private static Set<JavaCodeUnit> reachableFromApplyMethods(JavaClass aggregate) {
        Set<JavaCodeUnit> reached = new java.util.LinkedHashSet<>();
        java.util.Deque<JavaCodeUnit> pending = new java.util.ArrayDeque<>();
        aggregate.getCodeUnits().stream().filter(EntitiesRules::isApplyMethod).forEach(pending::add);
        while (!pending.isEmpty()) {
            JavaCodeUnit unit = pending.pop();
            if (!reached.add(unit)) {
                continue;
            }
            for (JavaMethodCall call : unit.getMethodCallsFromSelf()) {
                if (call.getTargetOwner().equals(aggregate)) {
                    aggregate.getCodeUnits().stream()
                            .filter(candidate -> candidate.getName().equals(call.getTarget().getName()))
                            .forEach(pending::add);
                }
            }
        }
        return reached;
    }

    /**
     * ES-3：套用事件的方法不檢查、不拒絕、不再產生事件。
     * 不只看套用方法本身，也看它呼叫到的每一個聚合自己的方法（間接丟例外一樣算）。
     */
    public static final ArchRule APPLY_METHODS_DO_NOT_REJECT = classes()
            .that(AGGREGATE_ROOT)
            .should(new ArchCondition<>("在 apply／on… 方法與它們呼叫到的自家方法裡，不丟例外、不呼叫 require…、不呼叫 record") {
                @Override
                public void check(JavaClass aggregate, ConditionEvents events) {
                    for (JavaCodeUnit unit : reachableFromApplyMethods(aggregate)) {
                        for (JavaConstructorCall call : unit.getConstructorCallsFromSelf()) {
                            // MatchException 是 javac 替「窮舉的 switch」自動產生的，不是程式自己丟的
                            boolean compilerGenerated = call.getTargetOwner().isEquivalentTo(MatchException.class);
                            if (call.getTargetOwner().isAssignableTo(Throwable.class) && !compilerGenerated) {
                                events.add(SimpleConditionEvent.violated(call, call.getDescription()));
                            }
                        }
                        for (JavaMethodCall call : unit.getMethodCallsFromSelf()) {
                            String target = call.getTarget().getName();
                            // 只看聚合自己的方法：javac 會替 switch 自動產生 Objects.requireNonNull，那不是業務檢查
                            boolean ownMethod = call.getTargetOwner().equals(aggregate);
                            if (ownMethod && (target.equals("record") || target.startsWith("require"))) {
                                events.add(SimpleConditionEvent.violated(call, call.getDescription()));
                            }
                        }
                    }
                }
            })
            .as("ES-3 套用事件的方法（含它呼叫到的自家方法）不得檢查規則、丟例外或再產生事件");

    /** ES-3：聚合的狀態只能在套用事件的方法裡改變。 */
    public static final ArchRule STATE_CHANGES_ONLY_IN_APPLY = classes()
            .that(AGGREGATE_ROOT)
            .should(new ArchCondition<>("只在 apply／on… 方法裡寫入自己的欄位，也不在別處修改集合內容") {
                private static final Set<String> BOOKKEEPING_FIELDS = Set.of("persistedVersion", "pendingEvents");
                /** 只負責「記下新事件／清掉已存的事件」的方法，可以動未提交事件清單。 */
                private static final Set<String> BOOKKEEPING_METHODS = Set.of("record", "markCommitted");
                private static final Set<String> COLLECTION_MUTATORS = Set.of("add", "addAll", "addFirst", "addLast",
                        "remove", "removeAll", "removeIf", "removeFirst", "removeLast", "retainAll", "clear", "set",
                        "sort", "replaceAll", "put", "putAll", "putIfAbsent", "compute", "computeIfAbsent",
                        "computeIfPresent", "merge", "push", "pop", "offer", "poll");

                @Override
                public void check(JavaClass aggregate, ConditionEvents events) {
                    for (JavaCodeUnit unit : aggregate.getCodeUnits()) {
                        if (isApplyMethod(unit)) {
                            continue;
                        }
                        for (JavaFieldAccess access : unit.getFieldAccesses()) {
                            boolean writesOwnState = access.getAccessType() == JavaFieldAccess.AccessType.SET
                                    && access.getTargetOwner().equals(aggregate)
                                    && !BOOKKEEPING_FIELDS.contains(access.getTarget().getName());
                            boolean initializer = unit.isConstructor() && access.getTarget().getName().equals("columns");
                            if (writesOwnState && !initializer) {
                                events.add(SimpleConditionEvent.violated(access, access.getDescription()));
                            }
                        }
                        // 狀態若放在可變集合裡，不必指定欄位也改得動——所以指令方法也不准呼叫集合的修改方法
                        if (BOOKKEEPING_METHODS.contains(unit.getName())) {
                            continue;
                        }
                        for (JavaMethodCall call : unit.getMethodCallsFromSelf()) {
                            boolean onCollection = call.getTargetOwner().isAssignableTo(Collection.class)
                                    || call.getTargetOwner().isAssignableTo(Map.class);
                            if (onCollection && COLLECTION_MUTATORS.contains(call.getTarget().getName())) {
                                events.add(SimpleConditionEvent.violated(call, call.getDescription()));
                            }
                        }
                    }
                }
            })
            .as("ES-3 聚合狀態只能經由套用事件改變");

    /** DDD-5：聚合之間不得持有彼此的物件參照（不同聚合套件互不相依）。 */
    public static final ArchRule AGGREGATE_REFERENCES_BY_ID = slices()
            .matching("..entities.(*)..")
            .should().notDependOnEachOther()
            .as("DDD-5 聚合之間只能用識別碼互相參照");

    /** ES-5：聚合測試只能透過 BoardScenario（Given／When／Then）操作聚合。 */
    public static final ArchRule AGGREGATE_TESTS_USE_SCENARIO = noClasses()
            .that().haveSimpleNameEndingWith("RulesTest").or().haveSimpleNameEndingWith("LifecycleTest")
            .should().callMethodWhere(new DescribedPredicate<JavaMethodCall>("直接重播事件或讀未提交事件") {
                @Override
                public boolean test(JavaMethodCall call) {
                    return Set.of("rehydrate", "uncommittedEvents", "markCommitted").contains(call.getTarget().getName());
                }
            })
            .as("ES-5 聚合測試一律寫成 Given 事件／When 指令／Then 事件或拒絕");
}
