package kanban.architecture.rules;

import com.tngtech.archunit.core.domain.JavaClass;

/** 各規則共用：辨認類別屬於哪一種結構。 */
final class Shapes {

    private Shapes() {
    }

    static boolean inPackage(JavaClass type, String segment) {
        return (type.getPackageName() + ".").contains("." + segment + ".");
    }

    /** Gateway 介面：Repository、事件存取、讀取模型的 Reader。 */
    static boolean isGatewayInterface(JavaClass type) {
        String name = type.getSimpleName();
        return type.isInterface()
                && (name.endsWith("Repository") || name.equals("EventStore") || name.equals("EventFeed")
                        || name.endsWith("Reader"));
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
