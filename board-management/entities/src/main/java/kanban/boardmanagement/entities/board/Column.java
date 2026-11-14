package kanban.boardmanagement.entities.board;

import java.util.List;

/** 欄位：看板聚合內部的物件，不外露（DDD-4）。wipLimit 為 null 代表無上限。 */
record Column(ColumnId id, ColumnName name, WipLimit wipLimit, List<Card> cards) {

    Column {
        cards = List.copyOf(cards);
    }

    Column withName(ColumnName newName) {
        return new Column(id, newName, wipLimit, cards);
    }

    Column withWipLimit(WipLimit newLimit) {
        return new Column(id, name, newLimit, cards);
    }

    Column withCards(List<Card> newCards) {
        return new Column(id, name, wipLimit, newCards);
    }

    int cardCount() {
        return cards.size();
    }
}
