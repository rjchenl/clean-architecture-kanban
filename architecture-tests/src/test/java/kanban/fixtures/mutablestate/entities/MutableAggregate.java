package kanban.fixtures.mutablestate.entities;

import java.util.ArrayList;
import java.util.List;

/** 違規樣本（ES-3）：狀態放在可變集合裡，指令方法不經過事件就直接改它（沒有任何欄位指定）。 */
public final class MutableAggregate {
    private final List<String> columns = new ArrayList<>();

    public void addColumn(String name) {
        columns.add(name);
    }

    public int columnCount() {
        return columns.size();
    }
}
