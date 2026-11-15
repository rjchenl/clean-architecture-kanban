package kanban.boardmanagement.usecases.queries.viewboard;

import java.util.List;

/** 讀取模型：一個看板的現況，為「看板現況」畫面準備（CQRS-3）。 */
public record BoardSnapshot(String boardId, String name, boolean archived, List<ColumnSnapshot> columns) {
}
