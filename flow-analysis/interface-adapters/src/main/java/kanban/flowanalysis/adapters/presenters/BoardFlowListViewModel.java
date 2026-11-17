package kanban.flowanalysis.adapters.presenters;

import java.util.List;

/** 看板流程清單（給定時檢查逐一送出用）。 */
public record BoardFlowListViewModel(List<String> boardFlowIds) {
}
