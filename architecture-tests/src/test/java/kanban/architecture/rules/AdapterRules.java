package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;

/** Interface Adapters 圈畫面側的架構規則（Controller、Presenter、ViewModel）。 */
public final class AdapterRules {

    private AdapterRules() {
    }

    /** CA-9：ViewModel 只含可以直接顯示的字串、布林、列舉，以及由它們組成的清單。 */
    public static final ArchRule VIEWMODELS_ARE_DISPLAY_READY = classes()
            .that().haveSimpleNameEndingWith("ViewModel")
            .should(new ArchCondition<>("是只含字串、布林、列舉與其他 ViewModel 清單的 record") {
                @Override
                public void check(JavaClass viewModel, ConditionEvents events) {
                    if (!viewModel.isRecord()) {
                        events.add(SimpleConditionEvent.violated(viewModel, viewModel.getName() + " 必須是 record"));
                    }
                    for (JavaField field : viewModel.getFields()) {
                        if (field.getModifiers().contains(JavaModifier.STATIC)) {
                            continue;
                        }
                        for (JavaClass used : field.getType().getAllInvolvedRawTypes()) {
                            boolean displayable = used.isEquivalentTo(String.class)
                                    || used.getName().equals("boolean")
                                    || used.isEnum()
                                    || used.isEquivalentTo(List.class)
                                    || used.getSimpleName().endsWith("ViewModel");
                            if (!displayable) {
                                events.add(SimpleConditionEvent.violated(field, field.getFullName()
                                        + " 的型別 " + used.getName() + " 不能直接顯示，要由 Presenter 先整理成文字"));
                            }
                        }
                    }
                }
            })
            .as("CA-9 ViewModel 只含可直接顯示的字串、布林、列舉");

    /** CA-11 DIP、CA-8：Controller 只透過 Input Boundary 與 Request Model 使用 Use Case，也不認識 Presenter。 */
    public static final ArchRule CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES = classes()
            .that().resideInAPackage("..adapters.controllers..")
            .should(new ArchCondition<>("只相依 Input Boundary 與 Request Model") {
                @Override
                public void check(JavaClass controller, ConditionEvents events) {
                    for (Dependency dependency : controller.getDirectDependenciesFromSelf()) {
                        JavaClass target = dependency.getTargetClass();
                        String name = target.getSimpleName();
                        boolean useCaseType = Shapes.inPackage(target, "usecases");
                        boolean allowedUseCaseType = name.endsWith("Input") || name.endsWith("Request");
                        boolean otherRingOrAdapter = Shapes.inPackage(target, "entities")
                                || (Shapes.inPackage(target, "adapters") && !Shapes.inPackage(target, "controllers"));
                        if ((useCaseType && !allowedUseCaseType) || otherRingOrAdapter) {
                            events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                        }
                    }
                }
            })
            .as("CA-11 DIP Controller 只相依 Input Boundary 與 Request Model");
}
