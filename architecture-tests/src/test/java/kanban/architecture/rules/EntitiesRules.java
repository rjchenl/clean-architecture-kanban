package kanban.architecture.rules;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;
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
}
