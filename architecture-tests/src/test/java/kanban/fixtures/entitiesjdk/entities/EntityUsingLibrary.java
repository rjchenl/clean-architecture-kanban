package kanban.fixtures.entitiesjdk.entities;

import org.assertj.core.api.SoftAssertions;

/** 違規樣本（CA-2）：Entities 圈用到 JDK 以外的函式庫。 */
public class EntityUsingLibrary {
    SoftAssertions library;
}
