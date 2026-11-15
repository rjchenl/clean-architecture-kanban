package kanban.fixtures.setter.entities;

/** 違規樣本（DDD-4）：聚合有公開 setter。 */
public final class SetterAggregate {
    private String name;

    public void setName(String name) {
        onRenamed(name);
    }

    private void onRenamed(String newName) {
        this.name = newName;
    }
}
