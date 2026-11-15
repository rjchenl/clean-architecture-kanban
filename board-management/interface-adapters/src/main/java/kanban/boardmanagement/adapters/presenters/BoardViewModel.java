package kanban.boardmanagement.adapters.presenters;

import java.util.List;

/** ViewModel：看板現況。只含可以直接顯示的字串、布林與清單（CA-9）。 */
public record BoardViewModel(boolean found, String boardId, String name, boolean archived, String archivedLabel,
                             List<ColumnViewModel> columns) {
}
