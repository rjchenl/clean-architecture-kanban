package kanban.boardmanagement.adapters.presenters;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryEntry;
import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryOutput;
import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryResponse;

/** Presenter：卡片移動歷程（CA-9）。時間依 Main 傳入的時區整理成文字，畫面不必再做任何轉換。 */
public final class CardHistoryPresenter implements ViewCardHistoryOutput {

    private final DateTimeFormatter timeFormat;
    private CardHistoryViewModel viewModel;

    public CardHistoryPresenter(ZoneId zone) {
        this.timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone);
    }

    @Override
    public void present(ViewCardHistoryResponse response) {
        if (!response.found()) {
            viewModel = new CardHistoryViewModel(false, "", List.of());
            return;
        }
        viewModel = new CardHistoryViewModel(true, response.history().title(),
                response.history().entries().stream()
                        .map(entry -> new CardHistoryLineViewModel(timeFormat.format(entry.occurredAt()), describe(entry)))
                        .toList());
    }

    public CardHistoryViewModel viewModel() {
        if (viewModel == null) {
            throw new IllegalStateException("Use Case 還沒有交出結果");
        }
        return viewModel;
    }

    private static String describe(CardHistoryEntry entry) {
        return switch (entry.kind()) {
            case ADDED -> "新增到「" + entry.toColumnName() + "」";
            case MOVED -> "從「" + entry.fromColumnName() + "」移到「" + entry.toColumnName() + "」";
            case REMOVED -> "已刪除";
        };
    }
}
