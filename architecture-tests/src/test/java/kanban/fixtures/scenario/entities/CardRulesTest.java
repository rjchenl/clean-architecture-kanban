package kanban.fixtures.scenario.entities;

import java.util.List;

/** 違規樣本（ES-5）：聚合測試繞過 Given／When／Then，自己重播事件。 */
public class CardRulesTest {

    void test() {
        FakeAggregate.rehydrate(List.of("event"));
    }
}
