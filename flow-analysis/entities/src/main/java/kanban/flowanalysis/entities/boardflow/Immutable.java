package kanban.flowanalysis.entities.boardflow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/** 不可變清單的小工具：每次都回傳新的清單，不改原本的（看板流程的狀態只在套用事件時整份換掉，ES-3）。 */
final class Immutable {

    private Immutable() {
    }

    static <T> List<T> appended(List<T> items, T item) {
        List<T> copy = new ArrayList<>(items);
        copy.add(item);
        return List.copyOf(copy);
    }

    /** 符合條件的那幾項換成 change 的結果，其餘不變。 */
    static <T> List<T> replaced(List<T> items, Predicate<T> which, UnaryOperator<T> change) {
        return items.stream().map(item -> which.test(item) ? change.apply(item) : item).toList();
    }
}
