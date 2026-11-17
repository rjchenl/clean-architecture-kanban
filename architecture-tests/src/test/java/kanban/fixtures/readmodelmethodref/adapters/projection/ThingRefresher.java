package kanban.fixtures.readmodelmethodref.adapters.projection;

/** 違規樣本（CQRS-5，2026-11-17 獨立審查 I-4）：不是 Projection，卻用方法參考清空讀取模型；只違反「方法參考也算寫入」。 */
public final class ThingRefresher {

    private final ReadModelStore store;

    public ThingRefresher(ReadModelStore store) {
        this.store = store;
    }

    public Runnable refresher() {
        return store::reset;
    }
}
