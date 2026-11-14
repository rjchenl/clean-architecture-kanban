package kanban.boardmanagement.adapters.presenters;

import java.util.List;

/** ViewModel：看板清單。 */
public record BoardListViewModel(List<BoardListItemViewModel> boards) {
}
