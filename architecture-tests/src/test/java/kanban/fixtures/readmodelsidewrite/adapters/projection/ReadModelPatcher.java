package kanban.fixtures.readmodelsidewrite.adapters.projection;

/** 違規樣本（CQRS-5）：projection 套件裡另一個不是 Projection 的類別偷改讀取模型。 */
public final class ReadModelPatcher {
    private final ReadModelStore store;

    public ReadModelPatcher(ReadModelStore store) {
        this.store = store;
    }

    public void patch() {
        store.saveBoard("patched");
    }
}
