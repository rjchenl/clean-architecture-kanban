package kanban.flowanalysis.usecases.queries.viewflowmetrics;

import java.time.Instant;

/** @param weekStartsAt 那一週的週一 00:00（以 Main 傳入的時區計） */
public record WeeklyCompletion(Instant weekStartsAt, int completedCount) {
}
