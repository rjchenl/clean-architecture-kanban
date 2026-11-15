package kanban.fixtures.adapterslibrary.adapters.eventstore;

import org.assertj.core.api.SoftAssertions;

/** 違規樣本（CA-7）：Interface Adapters 圈用了允許清單以外的函式庫（不在原本的禁用清單上）。 */
public final class UsesOtherLibrary {
    SoftAssertions library;
}
