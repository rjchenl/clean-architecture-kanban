package kanban.architecture.rules;

import com.tngtech.archunit.core.domain.JavaClass;

/** 各規則共用：辨認類別屬於哪一種結構。 */
final class Shapes {

    private Shapes() {
    }

    static boolean inPackage(JavaClass type, String segment) {
        return (type.getPackageName() + ".").contains("." + segment + ".");
    }

    /**
     * Gateway 介面：原本的名稱條件（Repository、事件存取、讀取模型的 Reader），
     * 或者形狀條件（規則目錄 B5）：Use Cases 圈的 top-level 介面，不在 impl、support，也不是 Input／Output Boundary。
     * 兩個條件任一成立就算——Use Case 向外要東西的介面不管叫什麼名字都會被認出來。
     */
    static boolean isGatewayInterface(JavaClass type) {
        String name = type.getSimpleName();
        boolean byName = name.endsWith("Repository") || name.equals("EventStore") || name.equals("EventFeed")
                || name.endsWith("Reader");
        boolean byShape = inPackage(type, "usecases") && type.isTopLevelClass()
                && !inPackage(type, "impl") && !inPackage(type, "support")
                && !name.endsWith("Input") && !name.endsWith("Output");
        return type.isInterface() && (byName || byShape);
    }

    static boolean implementsGateway(JavaClass type) {
        return !type.isInterface() && type.getAllRawInterfaces().stream().anyMatch(Shapes::isGatewayInterface);
    }

    /** 行為類別：不是 record、enum、介面、例外的一般類別。 */
    static boolean isBehaviourClass(JavaClass type) {
        return !type.isRecord() && !type.isEnum() && !type.isInterface() && !type.isAnnotation()
                && !type.isAssignableTo(Throwable.class);
    }
}
