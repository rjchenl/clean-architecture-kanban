package kanban.fixtures.scenarioanyname.entities;

import java.util.List;

/** 違規樣本（ES-5）：檔名不以 RulesTest／LifecycleTest 結尾的聚合測試，繞過 Given／When／Then 自己重播事件。 */
public class BoardMoveCheck {

    void test() {
        FakeAggregate.rehydrate(List.of("event"));
    }
}
