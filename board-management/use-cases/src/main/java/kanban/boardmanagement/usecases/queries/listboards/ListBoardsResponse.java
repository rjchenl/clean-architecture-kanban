package kanban.boardmanagement.usecases.queries.listboards;

import java.util.List;

/** Response Model：看板清單。 */
public record ListBoardsResponse(List<BoardSummary> boards) {
}
