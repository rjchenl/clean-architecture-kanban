package kanban.fixtures.scenario.entities;

import java.util.List;

public final class FakeAggregate {

    public static FakeAggregate rehydrate(List<String> history) {
        return new FakeAggregate();
    }
}
