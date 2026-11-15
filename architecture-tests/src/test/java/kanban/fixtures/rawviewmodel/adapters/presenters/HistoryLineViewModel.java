package kanban.fixtures.rawviewmodel.adapters.presenters;

import java.time.Instant;

/** 違規樣本（CA-9）：ViewModel 帶著還沒整理成文字的時間，畫面得自己轉格式。 */
public record HistoryLineViewModel(Instant occurredAt, String description) {
}
