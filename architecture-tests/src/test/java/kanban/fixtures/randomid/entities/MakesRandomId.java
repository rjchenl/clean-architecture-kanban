package kanban.fixtures.randomid.entities;

import java.util.UUID;

/** 違規樣本（ES-4）：領域模型自己產生亂數識別碼。 */
public record MakesRandomId(String value) {

    public static MakesRandomId next() {
        return new MakesRandomId(UUID.randomUUID().toString());
    }
}
