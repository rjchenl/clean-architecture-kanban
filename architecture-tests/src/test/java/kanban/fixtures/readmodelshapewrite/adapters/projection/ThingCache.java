package kanban.fixtures.readmodelshapewrite.adapters.projection;

import java.util.HashMap;
import java.util.Map;
import kanban.fixtures.readmodelshapewrite.usecases.queries.viewthing.ThingReader;

public final class ThingCache implements ThingReader {

    private final Map<String, String> values = new HashMap<>();

    @Override
    public String find(String id) {
        return values.get(id);
    }

    public void put(String id, String value) {
        values.put(id, value);
    }
}
