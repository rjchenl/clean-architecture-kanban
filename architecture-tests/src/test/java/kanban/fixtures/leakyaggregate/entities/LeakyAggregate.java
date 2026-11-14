package kanban.fixtures.leakyaggregate.entities;

import java.util.ArrayList;

/** 違規樣本（DDD-4）：聚合把內部的可變清單交給外部。 */
public final class LeakyAggregate {
    private final ArrayList<String> cards = new ArrayList<>();

    public ArrayList<String> cards() {
        return cards;
    }
}
