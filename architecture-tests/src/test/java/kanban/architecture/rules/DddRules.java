package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** DDD 的架構規則。規則編號對應專案 CLAUDE.md。 */
public final class DddRules {

    private DddRules() {
    }

    /** DDD-2：業務情境之間不得共用領域類別。正式程式碼用 {@link #CONTEXTS_DO_NOT_SHARE_DOMAIN}。 */
    public static ArchRule contextsDoNotShareDomain(String root) {
        return classes()
                .that().resideInAPackage(root + "..")
                .should(notUseAnotherContextsEntities(root))
                .as("DDD-2 業務情境之間不得引用彼此的領域模型");
    }

    public static final ArchRule CONTEXTS_DO_NOT_SHARE_DOMAIN = contextsDoNotShareDomain("kanban");

    /** DDD-8、CA-6：Gateway 介面在 Use Cases 圈，實作在 Interface Adapters 圈。 */
    public static final ArchRule REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE = classes()
            .that(new com.tngtech.archunit.base.DescribedPredicate<JavaClass>("是 Gateway 介面或它的實作") {
                @Override
                public boolean test(JavaClass type) {
                    return Shapes.isGatewayInterface(type) || Shapes.implementsGateway(type);
                }
            })
            .should(new ArchCondition<>("介面在 usecases、實作在 adapters") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    String expected = Shapes.isGatewayInterface(type) ? "usecases" : "adapters";
                    if (!Shapes.inPackage(type, expected)) {
                        events.add(SimpleConditionEvent.violated(type,
                                type.getName() + " 應該放在 " + expected + " 圈"));
                    }
                }
            })
            .as("DDD-8／CA-6 Gateway 介面在 Use Cases 圈，實作在 Interface Adapters 圈");

    private static ArchCondition<JavaClass> notUseAnotherContextsEntities(String root) {
        return new ArchCondition<>("不引用其他業務情境的 entities 套件") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                String originContext = contextOf(origin.getPackageName(), root);
                if (originContext == null || originContext.equals("main")) {
                    return;
                }
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    String targetContext = contextOf(targetPackage, root);
                    boolean targetIsDomain = targetContext != null
                            && (targetPackage + ".").startsWith(root + "." + targetContext + ".entities.");
                    if (targetIsDomain && !targetContext.equals(originContext)) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static String contextOf(String packageName, String root) {
        String prefix = root + ".";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String rest = packageName.substring(prefix.length());
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
