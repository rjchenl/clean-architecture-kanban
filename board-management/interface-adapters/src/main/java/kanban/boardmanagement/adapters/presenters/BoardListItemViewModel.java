package kanban.boardmanagement.adapters.presenters;

/** ViewModel：看板清單的一列。 */
public record BoardListItemViewModel(String boardId, String name, boolean archived, String archivedLabel) {
}
