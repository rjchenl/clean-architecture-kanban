package kanban.boardmanagement.usecases.queries.viewcardhistory.impl;

import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryReader;
import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryInput;
import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryOutput;
import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryRequest;
import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryResponse;

/** 查詢 Use Case：卡片移動歷程。只讀讀取模型，不讀事件紀錄（CQRS-3）。 */
public final class ViewCardHistoryUseCase implements ViewCardHistoryInput {

    private final CardHistoryReader histories;
    private final ViewCardHistoryOutput output;

    public ViewCardHistoryUseCase(CardHistoryReader histories, ViewCardHistoryOutput output) {
        this.histories = histories;
        this.output = output;
    }

    @Override
    public void execute(ViewCardHistoryRequest request) {
        if (request == null || request.boardId() == null || request.cardId() == null) {
            output.present(new ViewCardHistoryResponse(false, null));
            return;
        }
        output.present(histories.cardHistory(request.boardId(), request.cardId())
                .map(history -> new ViewCardHistoryResponse(true, history))
                .orElseGet(() -> new ViewCardHistoryResponse(false, null)));
    }
}
