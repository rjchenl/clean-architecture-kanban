package kanban.flowanalysis.usecases.commands.gateway;

import java.time.Instant;

/** 現在的時間。使用者的決定與定時檢查由 Use Case 從這裡取得時間再交給聚合，聚合自己不讀時鐘（ES-4）。 */
public interface TimeSource {

    Instant now();
}
