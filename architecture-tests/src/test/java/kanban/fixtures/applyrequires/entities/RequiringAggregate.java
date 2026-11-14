package kanban.fixtures.applyrequires.entities;

/** 違規樣本（ES-3）：在套用事件的方法裡呼叫自己的規則檢查。 */
public final class RequiringAggregate {
    private int count;

    private void requireRoom() {
    }

    private void onCardAdded() {
        requireRoom();
        count = count + 1;
    }

    public int count() {
        onCardAdded();
        return count;
    }
}
