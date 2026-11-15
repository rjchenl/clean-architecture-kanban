package kanban.boardmanagement.usecases.queries.listboards;

/** 讀取模型：看板清單裡的一列。 */
public record BoardSummary(String boardId, String name, boolean archived) {
}
