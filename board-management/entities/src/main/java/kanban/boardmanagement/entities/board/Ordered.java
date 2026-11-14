package kanban.boardmanagement.entities.board;

import java.util.ArrayList;
import java.util.List;

/** 有順序清單的不可變操作：每次都回傳新的清單。 */
final class Ordered {

    private Ordered() {
    }

    static <T> List<T> appended(List<T> items, T item) {
        List<T> copy = new ArrayList<>(items);
        copy.add(item);
        return List.copyOf(copy);
    }

    static <T> List<T> inserted(List<T> items, int index, T item) {
        List<T> copy = new ArrayList<>(items);
        copy.add(index, item);
        return List.copyOf(copy);
    }

    static <T> List<T> without(List<T> items, int index) {
        List<T> copy = new ArrayList<>(items);
        copy.remove(index);
        return List.copyOf(copy);
    }

    static <T> List<T> replaced(List<T> items, int index, T item) {
        List<T> copy = new ArrayList<>(items);
        copy.set(index, item);
        return List.copyOf(copy);
    }

    static <T> List<T> moved(List<T> items, int from, int to) {
        List<T> copy = new ArrayList<>(items);
        copy.add(to, copy.remove(from));
        return List.copyOf(copy);
    }
}
