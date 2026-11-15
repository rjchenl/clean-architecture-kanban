package kanban.fixtures.applyrejects.entities;

/** 違規樣本（ES-3）：在套用事件的方法裡檢查並丟例外。 */
public final class RejectingAggregate {
    private int count;

    private void onCardAdded(int limit) {
        if (count >= limit) {
            throw new IllegalStateException("over limit");
        }
        count = count + 1;
    }

    public void addCard(int limit) {
        onCardAdded(limit);
    }
}
