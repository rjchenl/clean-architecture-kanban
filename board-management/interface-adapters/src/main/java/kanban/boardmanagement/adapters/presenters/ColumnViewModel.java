package kanban.boardmanagement.adapters.presenters;

import java.util.List;

/** ViewModel：看板現況裡的一欄。cardCountLabel 是整理好的數量文字，例如「2 / 3」或「2」。 */
public record ColumnViewModel(String columnId, String name, String cardCountLabel, List<CardViewModel> cards) {
}
