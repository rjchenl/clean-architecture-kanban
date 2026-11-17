package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * 流程分析的判斷（看板流程的內部，不公開）：什麼算開始、什麼算完成、交付時間多長。
 * 每次都照目前的階段設定，從工作項目完整的停留紀錄從頭算（F6），所以改了設定，整段歷史跟著重算。
 */
final class FlowRules {

    private FlowRules() {
    }

    /**
     * 一個工作項目的流程結果（F2～F5、F11）。
     * <ol>
     *   <li>略過進出未分類欄的紀錄（F2：未分類不影響判斷）。</li>
     *   <li>開始時間＝剩下的紀錄中，第一次進入進行中或完成欄的時間（F3）。</li>
     *   <li>最後一筆在完成欄 → 完成時間＝結尾那一段連續完成紀錄的第一筆（F4：在完成欄之間移動不改完成時間；
     *       移回未開始或進行中就不算完成）。被刪除的工作項目照同一個算法，所以在完成欄被刪除的保留完成紀錄（F11）。</li>
     *   <li>交付時間＝完成時間－開始時間，兩者都有時才算（F5）；時間倒退時以 0 計。</li>
     * </ol>
     */
    static FlowResult resultOf(WorkItem item, Function<FlowColumnId, Stage> stageOf) {
        List<ColumnVisit> classified = item.visits().stream()
                .filter(visit -> stageOf.apply(visit.flowColumnId()) != Stage.UNCLASSIFIED)
                .toList();
        Instant startedAt = classified.stream()
                .filter(visit -> isStartedOrDone(stageOf.apply(visit.flowColumnId())))
                .findFirst()
                .map(ColumnVisit::enteredAt)
                .orElse(null);
        Instant completedAt = completedAt(classified, stageOf);
        LeadTime leadTime = startedAt != null && completedAt != null ? LeadTime.between(startedAt, completedAt) : null;
        return new FlowResult(startedAt, completedAt, leadTime);
    }

    private static Instant completedAt(List<ColumnVisit> classified, Function<FlowColumnId, Stage> stageOf) {
        int first = classified.size();
        while (first > 0 && stageOf.apply(classified.get(first - 1).flowColumnId()) == Stage.DONE) {
            first--;
        }
        return first < classified.size() ? classified.get(first).enteredAt() : null;
    }

    private static boolean isStartedOrDone(Stage stage) {
        return stage == Stage.IN_PROGRESS || stage == Stage.DONE;
    }

    /**
     * 算不算卡住（F8、F9、F11）：有門檻、沒被刪除、停在進行中的欄，而且在這一欄已經待滿門檻天數。
     * 其他階段待再久都不算。
     */
    static boolean isStuck(WorkItem item, Function<FlowColumnId, Stage> stageOf, StuckThreshold threshold,
            Instant now) {
        if (threshold == null || item.removed() || stageOf.apply(item.currentColumn()) != Stage.IN_PROGRESS) {
            return false;
        }
        Instant enteredAt = item.currentVisit().enteredAt();
        return !now.isBefore(enteredAt.plus(threshold.length()));
    }
}
