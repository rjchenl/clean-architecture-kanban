package kanban.fixtures.usecasesjdk.usecases;

import org.assertj.core.api.SoftAssertions;

/** 違規樣本（CA-3）：Use Cases 圈用到 JDK 以外的函式庫。 */
public class UseCaseUsingLibrary {
    SoftAssertions library;
}
