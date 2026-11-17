package kanban.fixtures.translationleaks.adapters.boardactivity;

import java.util.List;
import kanban.fixtures.translationleaks.adapters.projection.ThingStore;
import kanban.fixtures.translationleaks.usecases.queries.viewthing.ThingReader;

/** 違規樣本：防腐層直接寫 Projection 的儲存，又兼任查詢端的讀取。 */
public final class LeakyTranslator implements ThingReader {

    private final ThingStore store = null;

    @Override
    public List<String> things() {
        store.save("x");
        return List.of();
    }
}
