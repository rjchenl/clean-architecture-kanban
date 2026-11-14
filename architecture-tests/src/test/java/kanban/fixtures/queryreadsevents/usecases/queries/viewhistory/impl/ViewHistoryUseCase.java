package kanban.fixtures.queryreadsevents.usecases.queries.viewhistory.impl;

import kanban.fixtures.queryreadsevents.usecases.eventfeed.EventFeed;

/** 違規樣本（CQRS-3）：查詢 Use Case 直接讀事件紀錄來組歷程。 */
public final class ViewHistoryUseCase {
    EventFeed events;
}
