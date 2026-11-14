package kanban.boardmanagement.usecases.commands.gateway;

import java.time.Instant;

/** 現在時間的來源。聚合不讀時鐘，由 Use Case 取得後傳入（ES-4）。 */
public interface TimeSource {

    Instant now();
}
