package kanban.architecture.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Use Cases 圈的架構規則（邊界、Request／Response Model、套件命名）。規則編號對應專案 CLAUDE.md。 */
public final class UseCaseRules {

    private UseCaseRules() {
    }

    // ------------------------------------------------------------------ 辨認用的述詞

    private static DescribedPredicate<JavaClass> useCaseType(String description, java.util.function.Predicate<JavaClass> test) {
        return new DescribedPredicate<>(description) {
            @Override
            public boolean test(JavaClass type) {
                return Shapes.inPackage(type, "usecases") && type.isTopLevelClass() && test.test(type);
            }
        };
    }

    private static boolean isInput(JavaClass type) {
        return type.getSimpleName().endsWith("Input");
    }

    private static boolean isOutput(JavaClass type) {
        return type.getSimpleName().endsWith("Output");
    }

    private static boolean isBoundary(JavaClass type) {
        return isInput(type) || isOutput(type);
    }

    /** 跨越「畫面那一側」邊界的資料：指令端與查詢端套件裡的 record（Gateway、共用流程、實作套件除外）。 */
    private static boolean isBoundaryModel(JavaClass type) {
        boolean commandOrQuery = Shapes.inPackage(type, "commands") || Shapes.inPackage(type, "queries");
        boolean excluded = Shapes.inPackage(type, "gateway") || Shapes.inPackage(type, "support")
                || Shapes.inPackage(type, "impl");
        return type.isRecord() && commandOrQuery && !excluded;
    }

    private static boolean isSimple(JavaClass type) {
        if (type.isPrimitive() || type.isEnum() && Shapes.inPackage(type, "usecases")) {
            return true;
        }
        if (type.isRecord() && Shapes.inPackage(type, "usecases") && !Shapes.inPackage(type, "eventfeed")) {
            return true;
        }
        return type.isEquivalentTo(String.class) || type.isEquivalentTo(Integer.class)
                || type.isEquivalentTo(Boolean.class) || type.isEquivalentTo(Long.class)
                || type.isEquivalentTo(Instant.class) || type.isEquivalentTo(List.class);
    }

    private static String stem(JavaClass type, String suffix) {
        String name = type.getSimpleName();
        return name.substring(0, name.length() - suffix.length());
    }

    // ------------------------------------------------------------------ 規則

    /** CA-4：每個 Use Case 有 Input Boundary 與 Output Boundary；實作在 impl 套件，並持有自己的 Output Boundary。 */
    public static final ArchRule EVERY_USECASE_HAS_BOTH_BOUNDARIES = classes()
            .that(useCaseType("是 Input Boundary", UseCaseRules::isInput))
            .should(new ArchCondition<>("有對應的 Output Boundary 與實作") {
                @Override
                public void check(JavaClass input, ConditionEvents events) {
                    String name = stem(input, "Input");
                    boolean hasOutput = input.getPackage().getClasses().stream()
                            .anyMatch(c -> c.isInterface() && c.getSimpleName().equals(name + "Output"));
                    List<JavaClass> implementations = input.getAllSubclasses().stream()
                            .filter(c -> !c.isInterface())
                            .collect(Collectors.toList());
                    boolean oneImplementationInImpl = implementations.size() == 1
                            && implementations.get(0).getSimpleName().equals(name + "UseCase")
                            && implementations.get(0).getPackageName().equals(input.getPackageName() + ".impl");
                    boolean holdsOutput = oneImplementationInImpl && implementations.get(0).getAllFields().stream()
                            .anyMatch(f -> f.getRawType().getSimpleName().equals(name + "Output"));
                    if (!input.isInterface() || !hasOutput || !oneImplementationInImpl || !holdsOutput) {
                        events.add(SimpleConditionEvent.violated(input, input.getName()
                                + " 必須是介面，同套件要有 " + name + "Output，impl 套件要有唯一的 "
                                + name + "UseCase 並持有該 Output"));
                    }
                }
            })
            .as("CA-4 每個 Use Case 都有 Input Boundary、Output Boundary 與 impl 套件裡的實作");

    /** CA-11 ISP：邊界介面只有一個方法，不做成大介面。 */
    public static final ArchRule INPUT_BOUNDARY_HAS_ONE_METHOD = classes()
            .that(useCaseType("是邊界介面", UseCaseRules::isBoundary))
            .should(new ArchCondition<>("只有一個方法") {
                @Override
                public void check(JavaClass boundary, ConditionEvents events) {
                    if (boundary.getMethods().size() != 1) {
                        events.add(SimpleConditionEvent.violated(boundary,
                                boundary.getName() + " 有 " + boundary.getMethods().size() + " 個方法"));
                    }
                }
            })
            .as("CA-11 ISP 邊界介面只有一個方法");

    /** CA-5：跨越畫面側邊界的資料只能是簡單資料結構，不得出現 Entities 圈的型別。 */
    public static final ArchRule BOUNDARY_TYPES_ARE_SIMPLE = classes()
            .that(useCaseType("是邊界介面或 Request／Response Model",
                    type -> isBoundary(type) || isBoundaryModel(type)))
            .should(new ArchCondition<>("只使用簡單資料型別") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    if (isBoundary(type)) {
                        for (JavaMethod method : type.getMethods()) {
                            method.getParameterTypes().forEach(p -> report(type, p.getAllInvolvedRawTypes(), events));
                            report(type, method.getReturnType().getAllInvolvedRawTypes(), events);
                        }
                    } else {
                        for (JavaField field : type.getFields()) {
                            if (!field.getModifiers().contains(JavaModifier.STATIC)) {
                                report(type, field.getType().getAllInvolvedRawTypes(), events);
                            }
                        }
                    }
                }

                private void report(JavaClass owner, Set<JavaClass> involved, ConditionEvents events) {
                    for (JavaClass used : involved) {
                        boolean isVoid = used.getName().equals("void");
                        if (!isVoid && !isSimple(used) && !isBoundaryRequestOrResponse(used)) {
                            events.add(SimpleConditionEvent.violated(owner,
                                    owner.getName() + " 讓 " + used.getName() + " 穿過邊界"));
                        }
                    }
                }

                private boolean isBoundaryRequestOrResponse(JavaClass used) {
                    return isBoundaryModel(used);
                }
            })
            .as("CA-5 跨越邊界的資料只能是簡單資料結構");

    private static boolean isRepository(JavaClass type) {
        return type.getSimpleName().endsWith("Repository");
    }

    /** 指令 Use Case 自己，加上寫在它裡面的巢狀、匿名類別——它們的程式也屬於這個指令。 */
    private static Set<JavaClass> selfAndNested(JavaClass useCase) {
        Set<JavaClass> found = new java.util.LinkedHashSet<>();
        java.util.Deque<JavaClass> pending = new java.util.ArrayDeque<>(List.of(useCase));
        while (!pending.isEmpty()) {
            JavaClass current = pending.pop();
            if (!found.add(current)) {
                continue;
            }
            for (Dependency dependency : current.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                if (target.getName().startsWith(useCase.getName() + "$")) {
                    pending.add(target);
                }
            }
        }
        return found;
    }

    /**
     * DDD-5：一個指令只修改一個聚合。三件事一起守：
     * (1) 指令 Use Case 只持有一個 Repository；
     * (2) 不自己呼叫 Repository 的方法——載入、存回一律交給共用流程（BoardCommand），它只載入一個、存回同一個；
     * (3) 整個類別只把 Repository 交給別人一次，也就是只對一個聚合跑一次共用流程。
     * 在迴圈裡跑共用流程這種寫法看不出次數，由 Use Case 測試的 FakeBoardRepository 在每個指令測試結束時檢查。
     */
    public static final ArchRule ONE_AGGREGATE_PER_COMMAND = classes()
            .that(useCaseType("是指令 Use Case 的實作",
                    type -> Shapes.inPackage(type, "commands") && Shapes.inPackage(type, "impl")))
            .should(new ArchCondition<>("恰好持有一個 Repository、不自己呼叫它、只把它交給共用流程一次") {
                @Override
                public void check(JavaClass useCase, ConditionEvents events) {
                    long repositories = useCase.getAllFields().stream()
                            .filter(f -> isRepository(f.getRawType()))
                            .count();
                    if (repositories != 1) {
                        events.add(SimpleConditionEvent.violated(useCase,
                                useCase.getName() + " 持有 " + repositories + " 個 Repository"));
                    }
                    int handOvers = 0;
                    for (JavaClass part : selfAndNested(useCase)) {
                        // 方法呼叫、建構子呼叫、方法參考（boards::save）都算
                        List<JavaCodeUnitAccess<?>> accesses = new java.util.ArrayList<>();
                        accesses.addAll(part.getMethodCallsFromSelf());
                        accesses.addAll(part.getConstructorCallsFromSelf());
                        accesses.addAll(part.getCodeUnitReferencesFromSelf());
                        for (JavaCodeUnitAccess<?> access : accesses) {
                            if (isRepository(access.getTargetOwner())) {
                                events.add(SimpleConditionEvent.violated(access,
                                        access.getDescription() + "：指令自己操作 Repository，要交給共用流程"));
                            }
                            if (access.getTarget().getRawParameterTypes().stream().anyMatch(UseCaseRules::isRepository)) {
                                handOvers++;
                            }
                        }
                    }
                    if (handOvers != 1) {
                        events.add(SimpleConditionEvent.violated(useCase,
                                useCase.getName() + " 把 Repository 交給共用流程 " + handOvers + " 次，必須恰好一次"));
                    }
                }
            })
            .as("DDD-5 一個指令只修改一個聚合");

    /** CQRS-2：指令不帶回畫面資料——指令的 Output Boundary 只收 CommandResponse，而它只有三個欄位。 */
    public static final ArchRule COMMAND_RESPONSE_CARRIES_NO_VIEW_DATA = classes()
            .that(useCaseType("是指令端的 Output Boundary 或 CommandResponse",
                    type -> Shapes.inPackage(type, "commands")
                            && (isOutput(type) || type.getSimpleName().equals("CommandResponse"))))
            .should(new ArchCondition<>("只傳遞成功與否、失敗原因與識別碼") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    if (isOutput(type)) {
                        for (JavaMethod method : type.getMethods()) {
                            List<String> parameters = method.getRawParameterTypes().stream()
                                    .map(JavaClass::getSimpleName).collect(Collectors.toList());
                            if (!parameters.equals(List.of("CommandResponse"))) {
                                events.add(SimpleConditionEvent.violated(type,
                                        method.getFullName() + " 的參數必須恰為 CommandResponse"));
                            }
                        }
                    } else {
                        Set<String> fields = type.getFields().stream()
                                .filter(f -> !f.getModifiers().contains(JavaModifier.STATIC))
                                .map(JavaField::getName).collect(Collectors.toSet());
                        if (!fields.equals(Set.of("succeeded", "failureCode", "createdId"))) {
                            events.add(SimpleConditionEvent.violated(type,
                                    "CommandResponse 的欄位必須恰為 succeeded、failureCode、createdId，實際：" + fields));
                        }
                    }
                }
            })
            .as("CQRS-2 指令的 Response Model 只含成功或失敗、失敗原因與必要的識別碼");

    /**
     * CA-16、CA-4：impl 套件裡只有 Use Case 的實作類別——final、名稱以 UseCase 結尾，
     * 而且實作上一層套件裡同名的 Input Boundary。沒有實作 Input 的類別不准放在這裡。
     */
    public static final ArchRule NO_UNNECESSARY_PUBLIC = classes()
            .that(useCaseType("在 impl 套件", type -> Shapes.inPackage(type, "impl")))
            .should(new ArchCondition<>("是 final 的 …UseCase，並實作同名的 …Input") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    String name = type.getSimpleName();
                    boolean named = name.endsWith("UseCase");
                    String expectedInput = named ? stem(type, "UseCase") + "Input" : "";
                    String parentPackage = type.getPackageName().substring(0, type.getPackageName().length() - ".impl".length());
                    boolean implementsOwnInput = type.getRawInterfaces().stream()
                            .anyMatch(i -> i.getSimpleName().equals(expectedInput) && i.getPackageName().equals(parentPackage));
                    if (!named || !type.getModifiers().contains(JavaModifier.FINAL) || !implementsOwnInput) {
                        events.add(SimpleConditionEvent.violated(type, type.getName()
                                + " 必須是 final 的 …UseCase，並實作 " + parentPackage + " 的同名 …Input"));
                    }
                }
            })
            .as("CA-16／CA-4 impl 套件只放實作了自己 Input Boundary 的 final Use Case 類別");

    private static final String COMMAND_USE_CASES = "createboard|renameboard|archiveboard|addcolumn|renamecolumn"
            + "|movecolumn|setcolumnwiplimit|removecolumnwiplimit|removecolumn|addcard|changecardcontent|movecard"
            + "|repositioncard|removecard"
            // 流程分析的指令（第二階段設計第 0 節 4d）
            + "|recordboardactivity|setcolumnstage|setstuckthreshold|removestuckthreshold|checkstuckworkitems";
    private static final String QUERY_USE_CASES = "viewboard|listboards|viewcardhistory"
            // 看板管理對其他業務情境公布的看板動態（設計第 0 節 4d）
            + "|readboardactivities";

    /** 允許清單寫死在這裡：新增 Use Case 時要同時改這份清單（CA-10）。 */
    private static final Pattern ALLOWED_USE_CASE_PACKAGES = Pattern.compile(".*\\.usecases\\.("
            + "commands|commands\\.gateway|commands\\.support|eventfeed"
            + "|commands\\.(" + COMMAND_USE_CASES + ")(\\.impl)?"
            + "|queries\\.(" + QUERY_USE_CASES + ")(\\.impl)?)");

    /** CA-10：Use Cases 圈的套件以 Use Case 的業務名稱命名，結構性名稱只允許清單上的四個。 */
    public static final ArchRule USECASE_PACKAGES_ON_ALLOW_LIST = classes()
            .that(useCaseType("在 Use Cases 圈", type -> true))
            .should(new ArchCondition<>("放在允許清單上的套件") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    if (!ALLOWED_USE_CASE_PACKAGES.matcher(type.getPackageName()).matches()) {
                        events.add(SimpleConditionEvent.violated(type,
                                type.getName() + " 所在的套件不在 Use Cases 圈的允許清單內"));
                    }
                }
            })
            .as("CA-10 Use Cases 圈的套件以 Use Case 的業務名稱命名");

    private static final Pattern COMMAND_USE_CASE_PACKAGE = Pattern.compile(".*\\.usecases\\.commands\\.(?!gateway$|support$)\\w+");
    private static final Pattern QUERY_USE_CASE_PACKAGE = Pattern.compile(".*\\.usecases\\.queries\\.\\w+");

    /**
     * CQRS-4、ES-1：Use Case 自己的套件裡只能放邊界介面與 Request／Response Model。
     * 新的 Gateway 不能偷放在這裡——指令端的 Gateway 只能放在 commands.gateway（那裡另有允許清單）。
     * 查詢端另外允許唯讀的 …Reader 與讀取模型的 record／enum。
     */
    public static final ArchRule USECASE_PACKAGES_HOLD_ONLY_BOUNDARY_TYPES = classes()
            .that(useCaseType("直接放在某個 Use Case 的套件裡", type ->
                    COMMAND_USE_CASE_PACKAGE.matcher(type.getPackageName()).matches()
                            || QUERY_USE_CASE_PACKAGE.matcher(type.getPackageName()).matches()))
            .should(new ArchCondition<>("是邊界介面或 Request／Response Model") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    String name = type.getSimpleName();
                    boolean command = COMMAND_USE_CASE_PACKAGE.matcher(type.getPackageName()).matches();
                    boolean allowed = command
                            ? (type.isInterface() && (name.endsWith("Input") || name.endsWith("Output")))
                                    || (type.isRecord() && name.endsWith("Request"))
                            : (type.isInterface()
                                    && (name.endsWith("Input") || name.endsWith("Output") || name.endsWith("Reader")))
                                    || type.isRecord() || type.isEnum();
                    if (!allowed) {
                        events.add(SimpleConditionEvent.violated(type, type.getName()
                                + " 不是邊界介面或 Request／Response Model，不能放在 Use Case 的套件裡"));
                    }
                }
            })
            .as("CQRS-4／ES-1 Use Case 套件裡只放邊界介面與 Request／Response Model");

    /**
     * CQRS-4：指令 Use Case 只能使用寫入端的 Gateway（commands.gateway 套件）與自己的 Output Boundary。
     * 欄位型別走允許清單，所以拿不到任何能讀取讀取模型的東西。
     */
    public static final ArchRule COMMAND_USECASES_USE_ONLY_WRITE_GATEWAYS = classes()
            .that(useCaseType("是指令 Use Case 的實作",
                    type -> Shapes.inPackage(type, "commands") && Shapes.inPackage(type, "impl")))
            .should(new ArchCondition<>("欄位只有寫入端的 Gateway 與自己的 Output Boundary") {
                @Override
                public void check(JavaClass useCase, ConditionEvents events) {
                    for (JavaField field : useCase.getAllFields()) {
                        JavaClass fieldType = field.getRawType();
                        boolean writeGateway = Shapes.inPackage(fieldType, "commands") && Shapes.inPackage(fieldType, "gateway");
                        boolean ownOutput = fieldType.isInterface() && fieldType.getSimpleName().endsWith("Output")
                                && Shapes.inPackage(fieldType, "commands");
                        if (!writeGateway && !ownOutput) {
                            events.add(SimpleConditionEvent.violated(field, field.getFullName() + " 的型別 "
                                    + fieldType.getName() + " 不是寫入端的 Gateway，也不是 Output Boundary"));
                        }
                    }
                }
            })
            .as("CQRS-4 指令 Use Case 只能使用寫入端的 Gateway 與自己的 Output Boundary");
}
