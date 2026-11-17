package kanban.flowanalysis.usecases.queries.listboardflows;

import java.util.List;

/** @param boardFlowIds 依第一次出現的順序 */
public record ListBoardFlowsResponse(List<String> boardFlowIds) {
}
