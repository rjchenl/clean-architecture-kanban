package kanban.fixtures.numericviewmodel.adapters.presenters;

/** 違規樣本（CA-9）：ViewModel 帶著張數與上限兩個數字，等於叫畫面自己判斷滿了沒。 */
public record ColumnViewModel(String name, int cardCount, Integer wipLimit) {
}
