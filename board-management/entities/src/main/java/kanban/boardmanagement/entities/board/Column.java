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

    /** 卡片在這一欄的位置；不在這一欄回傳 -1。 */
    int indexOfCard(CardId cardId) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).id().equals(cardId)) {
                return i;
            }
        }
        return -1;
    }

    int cardCount() {
        return cards.size();
    }
}
