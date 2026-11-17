package kanban.fixtures.readmodelshapewrite.adapters.projection;

/** 違規樣本（CQRS-5，2026-11-17 獨立審查 I-4）：不是 Projection，卻呼叫讀取模型儲存自己的寫入方法；只違反「寫入方法依形狀認」。 */
public final class ThingFiller {

    private final ThingCache cache;

    public ThingFiller(ThingCache cache) {
        this.cache = cache;
    }

    public void fill() {
        cache.put("thing-1", "filled");
    }
}
