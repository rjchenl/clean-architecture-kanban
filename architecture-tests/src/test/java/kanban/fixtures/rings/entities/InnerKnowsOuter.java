package kanban.fixtures.rings.entities;

import kanban.fixtures.rings.usecases.OuterThing;

/** 違規樣本（CA-1）：Entities 圈引用 Use Cases 圈。 */
public class InnerKnowsOuter {
    OuterThing outer;
}
