package kanban.fixtures.ctorreference.adapters.controllers;

import java.util.function.Supplier;
import kanban.fixtures.ctorreference.adapters.eventstore.SomeStore;

/** 違規樣本（CA-8）：用建構子參考（::new）自己建立轉接程式，繞過「只有 Main 建立」。 */
public final class RefController {
    private final Supplier<SomeStore> factory = SomeStore::new;

    public SomeStore make() {
        return factory.get();
    }
}
