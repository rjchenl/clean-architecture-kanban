package kanban.architecture.support;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** 讀 docs/glossary.md 的「程式名稱」欄，並找出不在用語表裡的領域名稱（DDD-1）。 */
public final class Glossary {

    private static final Path FILE = Path.of("..", "docs", "glossary.md");

    private Glossary() {
    }

    /** 用語表「程式名稱」欄的全部名稱。 */
    public static Set<String> codeNames() {
        return codeNamesWhereKind(kind -> true);
    }

    /** 「種類」欄恰好是 kind 的那幾列的程式名稱（例如「聚合內部物件」）。 */
    public static Set<String> codeNamesOfKind(String kind) {
        return codeNamesWhereKind(kind::equals);
    }

    private static Set<String> codeNamesWhereKind(java.util.function.Predicate<String> kindMatches) {
        Set<String> names = new LinkedHashSet<>();
        try {
            for (String line : Files.readAllLines(FILE)) {
                String[] cells = line.split("\\|");
                // 表格列：| 中文用語 | 程式名稱 | 種類 | 意思 |
                if (cells.length < 5 || cells[2].isBlank() || cells[2].contains("---")) {
                    continue;
                }
                if (!kindMatches.test(cells[3].strip())) {
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

    /**
     * 某個業務情境那一節（「## 業務情境：…（模組目錄）」底下的表格）的程式名稱。
     *
     * @param context 模組目錄名去掉「-」，和套件裡的情境名稱相同，例如 {@code flowanalysis}
     */
    public static Set<String> codeNamesIn(String context) {
        return codeNamesByContext().getOrDefault(context, Set.of());
    }

    private static Map<String, Set<String>> codeNamesByContext() {
        Map<String, Set<String>> byContext = new HashMap<>();
        String currentContext = null;
        try {
            for (String line : Files.readAllLines(FILE)) {
                if (line.startsWith("## ")) {
                    currentContext = contextOfHeading(line);
                    continue;
                }
                String[] cells = line.split("\\|");
                if (currentContext == null || cells.length < 5 || cells[2].isBlank() || cells[2].contains("---")
                        || cells[2].strip().equals("程式名稱")) {
                    continue;
                }
                Set<String> names = byContext.computeIfAbsent(currentContext, key -> new LinkedHashSet<>());
                for (String name : cells[2].split("／")) {
                    names.add(name.strip());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("讀不到用語表 " + FILE.toAbsolutePath(), e);
        }
        return byContext;
    }

    /** 「## 業務情境：流程分析（flow-analysis）」→ flowanalysis；其他標題回 null。 */
    private static String contextOfHeading(String heading) {
        int open = heading.lastIndexOf('（');
        int close = heading.lastIndexOf('）');
        if (!heading.startsWith("## 業務情境：") || open < 0 || close < open) {
            return null;
        }
        return heading.substring(open + 1, close).replace("-", "");
    }

    /** 必須出現在用語表的名稱：Entities 圈的公開頂層型別、指令端的 Request（去掉 Request）。 */
    public static Set<String> namesThatMustBeListed(JavaClasses classes) {
        Set<String> required = new TreeSet<>();
        for (JavaClass type : classes) {
            String name = requiredName(type);
            if (name != null) {
                required.add(name);
            }
        }
        return required;
    }

    private static String requiredName(JavaClass type) {
        if (!type.isTopLevelClass()) {
            return null;
        }
        String packageName = type.getPackageName() + ".";
        String simpleName = type.getSimpleName();
        if (packageName.contains(".entities.") && type.getModifiers().contains(JavaModifier.PUBLIC)) {
            return simpleName;
        }
        if (packageName.contains(".usecases.commands.") && simpleName.endsWith("Request")) {
            return simpleName.substring(0, simpleName.length() - "Request".length());
        }
        return null;
    }

    /** 應該在用語表卻不在的名稱（不分情境，任何一節有列就算）。 */
    public static Set<String> missing(JavaClasses classes) {
        Set<String> missing = namesThatMustBeListed(classes);
        missing.removeAll(codeNames());
        return missing;
    }

    /**
     * DDD-1、DDD-2：每個業務情境的名稱必須列在用語表裡自己那一節——同一個詞在不同情境可以有不同意義，
     * 只列在別的情境不算。類別的情境＝root 之後的第一段；main 與不屬於任何情境的類別不檢查。
     *
     * @return 「情境: 名稱」
     */
    public static Set<String> missingPerContext(JavaClasses classes, String root) {
        Set<String> missing = new TreeSet<>();
        String prefix = root + ".";
        for (JavaClass type : classes) {
            String name = requiredName(type);
            String packageName = type.getPackageName();
            if (name == null || !packageName.startsWith(prefix)) {
                continue;
            }
            String rest = packageName.substring(prefix.length());
            String context = rest.contains(".") ? rest.substring(0, rest.indexOf('.')) : rest;
            if (context.equals("main")) {
                continue;
            }
            if (!codeNamesIn(context).contains(name)) {
                missing.add(context + ": " + name);
            }
        }
        return missing;
    }
}
