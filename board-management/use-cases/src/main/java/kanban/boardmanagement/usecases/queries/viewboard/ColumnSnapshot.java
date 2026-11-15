package kanban.boardmanagement.usecases.queries.viewboard;

import java.util.List;

/** 讀取模型：看板現況裡的一欄。wipLimit 為 null 代表無上限。 */
public record ColumnSnapshot(String columnId, String name, Integer wipLimit, List<CardSnapshot> cards) {
}
