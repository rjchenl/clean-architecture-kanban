package kanban.fixtures.publiclane.entities;

/** 違規樣本（DDD-4）：新增的聚合內部實體（泳道）宣告成 public；名稱不是 Column 也不是 Card。 */
public final class Lane {

    private String name;

    void rename(String newName) {
        this.name = newName;
    }
}
