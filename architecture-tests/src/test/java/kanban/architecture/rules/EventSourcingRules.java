package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

/** Event Sourcing 的架構規則。規則編號對應專案 CLAUDE.md。 */
public final class EventSourcingRules {

    private EventSourcingRules() {
    }

    private static final Set<String> WRITE_SIDE_GATEWAY_TYPES = Set.of(
            "BoardRepository", "EventStore", "TimeSource", "IdentityGenerator", "ConcurrentUpdateException");

    /**
     * ES-1：寫入端只有事件紀錄，沒有另存「聚合現況」的途徑。
     * (1) 指令端的 Gateway 套件只允許固定的幾個型別，多一個就要先改這條規則；
     * (2) Repository 的實作只能靠 EventStore 存取資料，不得直接碰資料庫。
     */
    public static final ArchRule NO_STATE_TABLE_ABSTRACTION_ON_WRITE_SIDE = classes()
            .that(new DescribedPredicate<JavaClass>("是指令端的 Gateway 型別或 Repository 的實作") {
                @Override
                public boolean test(JavaClass type) {
                    return (Shapes.inPackage(type, "commands") && Shapes.inPackage(type, "gateway"))
                            || isRepositoryImplementation(type);
                }
            })
            .should(new ArchCondition<>("不提供、也不使用事件紀錄以外的寫入途徑") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    if (isRepositoryImplementation(type)) {
                        boolean usesEventStore = type.getAllFields().stream()
                                .anyMatch(f -> f.getRawType().getSimpleName().equals("EventStore"));
                        boolean touchesDatabase = type.getDirectDependenciesFromSelf().stream()
                                .anyMatch(d -> d.getTargetClass().getPackageName().startsWith("java.sql")
                                        || d.getTargetClass().getPackageName().startsWith("javax.sql"));
                        if (!usesEventStore || touchesDatabase) {
                            events.add(SimpleConditionEvent.violated(type,
                                    type.getName() + " 必須只透過 EventStore 存取資料"));
                        }
                    } else if (type.isTopLevelClass() && !WRITE_SIDE_GATEWAY_TYPES.contains(type.getSimpleName())) {
                        events.add(SimpleConditionEvent.violated(type,
                                type.getName() + " 不在指令端 Gateway 的允許清單內"));
                    }
                }
            })
            .as("ES-1 寫入端只有事件紀錄，不得另存聚合現況");

    private static final Set<String> JDBC_USERS = Set.of("PostgresEventStore", "PostgresReadModelStore");

    /**
     * ES-1：Interface Adapters 圈只有 PostgreSQL 版的事件庫與讀取模型可以碰資料庫。
     * 其他類別一用到 java.sql／javax.sql 就違規——所以沒有地方能偷偷另存一份「看板現況」。
     */
    public static final ArchRule JDBC_ONLY_IN_POSTGRES_STORES = classes()
            .that().resideInAPackage("..adapters..")
            .should(new ArchCondition<>("不使用 JDBC（事件庫與讀取模型的 PostgreSQL 版除外）") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    JavaClass topLevel = type;
                    while (topLevel.getEnclosingClass().isPresent()) {
                        topLevel = topLevel.getEnclosingClass().get();
                    }
                    if (JDBC_USERS.contains(topLevel.getSimpleName())) {
                        return;
                    }
                    type.getDirectDependenciesFromSelf().stream()
                            .filter(d -> d.getTargetClass().getPackageName().startsWith("java.sql")
                                    || d.getTargetClass().getPackageName().startsWith("javax.sql"))
                            .forEach(d -> events.add(SimpleConditionEvent.violated(d, d.getDescription())));
                }
            })
            .as("ES-1 只有 PostgresEventStore 與 PostgresReadModelStore 可以使用 JDBC");

    private static boolean isRepositoryImplementation(JavaClass type) {
        return !type.isInterface()
                && type.getAllRawInterfaces().stream().anyMatch(i -> i.getSimpleName().endsWith("Repository"));
    }
}
