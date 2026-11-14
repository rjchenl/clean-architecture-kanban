package kanban.fixtures.directstate.entities;

/** 違規樣本（ES-3）：指令方法不經過事件，直接改狀態。 */
public final class DirectAggregate {
    private String name;

    public void rename(String newName) {
        this.name = newName;
    }

    public String describe() {
        return name;
    }
}
