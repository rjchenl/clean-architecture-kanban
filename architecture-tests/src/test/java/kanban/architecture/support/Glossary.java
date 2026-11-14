package kanban.architecture.support;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/** 讀 docs/glossary.md 的「程式名稱」欄，並找出不在用語表裡的領域名稱（DDD-1）。 */
public final class Glossary {

    private static final Path FILE = Path.of("..", "docs", "glossary.md");

    private Glossary() {
    }

    /** 用語表「程式名稱」欄的全部名稱。 */
    public static Set<String> codeNames() {
        Set<String> names = new LinkedHashSet<>();
        try {
            for (String line : Files.readAllLines(FILE)) {
                String[] cells = line.split("\\|");
                // 表格列：| 中文用語 | 程式名稱 | 種類 | 意思 |
                if (cells.length < 5 || cells[2].isBlank() || cells[2].contains("---")) {
                    continue;
                }
                for (String name : cells[2].split("／")) {
                    names.add(name.strip());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("讀不到用語表 " + FILE.toAbsolutePath(), e);
        }
        return names;
    }

    /** 必須出現在用語表的名稱：Entities 圈的公開頂層型別、指令端的 Request（去掉 Request）。 */
    public static Set<String> namesThatMustBeListed(JavaClasses classes) {
        Set<String> required = new TreeSet<>();
        for (JavaClass type : classes) {
            if (!type.isTopLevelClass()) {
                continue;
            }
            String packageName = type.getPackageName() + ".";
            String simpleName = type.getSimpleName();
            if (packageName.contains(".entities.") && type.getModifiers().contains(JavaModifier.PUBLIC)) {
                required.add(simpleName);
            }
            if (packageName.contains(".usecases.commands.") && simpleName.endsWith("Request")) {
                required.add(simpleName.substring(0, simpleName.length() - "Request".length()));
            }
        }
        return required;
    }

    /** 應該在用語表卻不在的名稱。 */
    public static Set<String> missing(JavaClasses classes) {
        Set<String> missing = namesThatMustBeListed(classes);
        missing.removeAll(codeNames());
        return missing;
    }
}
