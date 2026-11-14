package kanban.boardmanagement.adapters.presenters;

import java.util.List;

/** ViewModel：卡片移動歷程。 */
public record CardHistoryViewModel(boolean found, String cardTitle, List<CardHistoryLineViewModel> lines) {
}
