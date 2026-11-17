package kanban.flowanalysis.usecases.queries.viewflowmetrics;

import java.time.Instant;

/** @param stuckSince 從什麼時候開始停在這一欄 */
public record StuckWorkItem(String workItemId, String title, String flowColumnName, Instant stuckSince) {
}
