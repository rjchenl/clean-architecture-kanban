package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Map;
import java.util.Set;

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

    /**
     * DDD-3、CA-16：業務情境之間只透過上游公布的格式整合，而且下游只有防腐層可以碰它（設計第 4.4 節）。
     *
     * <p>兩張表是 docs/context-map.md 的程式版：
     * {@code publishedLanguages} 是每個上游公布給別人讀的套件；
     * {@code translators} 是下游的哪個套件負責翻譯哪個上游。
     * 情境 X 的類別引用情境 Y 的類別時，自己必須在 X 翻譯 Y 的套件裡，目標必須是 Y 公布的套件（不含子套件）。
     * 表上沒有列的方向一律違規，例如上游引用下游。Main 負責組裝全部，不在此限。
     */
    public static ArchRule contextsIntegrateOnlyThroughPublishedLanguage(String root,
            Map<String, Set<String>> publishedLanguages, Map<String, Map<String, String>> translators) {
        return classes()
                .that().resideInAPackage(root + "..")
                .should(new ArchCondition<>("只在防腐層、而且只透過上游公布的格式引用其他業務情境") {
                    @Override
                    public void check(JavaClass origin, ConditionEvents events) {
                        String originContext = contextOf(origin.getPackageName(), root);
                        if (originContext == null || originContext.equals("main")) {
                            return;
                        }
                        for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                            String targetPackage = dependency.getTargetClass().getPackageName();
                            String targetContext = contextOf(targetPackage, root);
                            if (targetContext == null || targetContext.equals(originContext)
                                    || targetContext.equals("main")) {
                                continue;
                            }
                            String translator = translators.getOrDefault(originContext, Map.of()).get(targetContext);
                            boolean fromTranslator = translator != null && (origin.getPackageName() + ".")
                                    .startsWith(root + "." + originContext + "." + translator + ".");
                            boolean toPublished = publishedLanguages.getOrDefault(targetContext, Set.of()).stream()
                                    .anyMatch(published -> targetPackage.equals(root + "." + targetContext + "." + published));
                            if (!fromTranslator || !toPublished) {
                                events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                            }
                        }
                    }
                })
                .as("DDD-3 業務情境之間只透過上游公布的格式整合，而且下游只有防腐層可以引用");
    }

    public static final ArchRule CONTEXTS_INTEGRATE_ONLY_THROUGH_PUBLISHED_LANGUAGE =
            contextsIntegrateOnlyThroughPublishedLanguage("kanban",
                    Map.of("boardmanagement", Set.of("usecases.queries.readboardactivities")),
                    Map.of("flowanalysis", Map.of("boardmanagement", "adapters.boardactivity")));

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
