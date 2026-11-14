package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;

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
}
