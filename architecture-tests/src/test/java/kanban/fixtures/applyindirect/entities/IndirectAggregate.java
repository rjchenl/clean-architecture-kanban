package kanban.fixtures.applyindirect.entities;

/** 違規樣本（ES-3）：套用事件的方法透過另一個自家方法間接檢查並丟例外。 */
public final class IndirectAggregate {
    private int count;

    private void ensureRoom() {
        if (count > 3) {
            throw new IllegalStateException("full");
        }
    }

    private void onCardAdded() {
        ensureRoom();
        count = count + 1;
    }

    public int add() {
        onCardAdded();
        return count;
    }
}
