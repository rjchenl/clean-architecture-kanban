package kanban.fixtures.readmodeloutside.adapters.system;

import java.util.HashMap;
import java.util.Map;
import kanban.fixtures.readmodeloutside.usecases.queries.viewthing.ThingReader;

/** 違規樣本（CQRS-5，2026-11-17 獨立審查 I-3）：讀取模型的儲存換到 projection 以外的套件。 */
public final class MetricsCache implements ThingReader {

    private final Map<String, String> values = new HashMap<>();

    @Override
    public String find(String id) {
        return values.get(id);
    }

    public void put(String id, String value) {
        values.put(id, value);
    }
}
