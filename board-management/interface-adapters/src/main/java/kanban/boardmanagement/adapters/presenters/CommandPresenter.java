package kanban.boardmanagement.adapters.presenters;

import java.util.Map;
import kanban.boardmanagement.usecases.commands.CommandResponse;

/**
 * 所有指令 Presenter 共用的整理方式：把指令的 Response Model 整理成 ViewModel（CA-9）。
 * 所有給使用者看的文字都在這裡；這裡不判斷指令能不能做，只把聚合給的結果翻成訊息。
 *
 * <p>每個指令有自己的 Presenter 類別（例如 MoveCardPresenter）實作自己的 Output Boundary，
 * 新增指令時新增一個類別即可，不必修改既有的 Presenter（CA-11 OCP）。
 */
public class CommandPresenter {

    private static final String INVALID_REQUEST_MESSAGE = "請求內容不完整或格式不正確";

    private record Wording(CommandOutcome outcome, String message) {
    }

    private static final Map<String, Wording> WORDINGS = Map.ofEntries(
            Map.entry("BOARD_NOT_FOUND", new Wording(CommandOutcome.NOT_FOUND, "找不到這個看板")),
            Map.entry("COLUMN_NOT_FOUND", new Wording(CommandOutcome.NOT_FOUND, "找不到這個欄位")),
            Map.entry("CARD_NOT_FOUND", new Wording(CommandOutcome.NOT_FOUND, "找不到這張卡片")),
            Map.entry("CONCURRENT_UPDATE", new Wording(CommandOutcome.CONFLICT, "看板已被更新，請重新整理後再試")),
            Map.entry("BOARD_ARCHIVED", new Wording(CommandOutcome.REJECTED, "看板已封存，不能再變更")),
            Map.entry("WIP_LIMIT_EXCEEDED", new Wording(CommandOutcome.REJECTED, "這一欄已達在製品上限，放不進去")),
            Map.entry("WIP_LIMIT_BELOW_CARD_COUNT",
                    new Wording(CommandOutcome.REJECTED, "上限不能低於欄內現有的卡片數，請先移走卡片")),
            Map.entry("COLUMN_NOT_EMPTY", new Wording(CommandOutcome.REJECTED, "欄內還有卡片，不能刪除")),
            Map.entry("COLUMN_NAME_DUPLICATED", new Wording(CommandOutcome.REJECTED, "這個看板已經有同名的欄位")),
            Map.entry("NAME_BLANK", new Wording(CommandOutcome.REJECTED, "名稱不可空白")),
            Map.entry("NAME_TOO_LONG", new Wording(CommandOutcome.REJECTED, "名稱最長 100 字")),
            Map.entry("DESCRIPTION_TOO_LONG", new Wording(CommandOutcome.REJECTED, "說明最長 2000 字")),
            Map.entry("WIP_LIMIT_NOT_POSITIVE", new Wording(CommandOutcome.REJECTED, "在製品上限必須是 1 以上的整數")),
            Map.entry("POSITION_OUT_OF_RANGE", new Wording(CommandOutcome.REJECTED, "位置超出範圍")),
            Map.entry("INVALID_CHARACTERS", new Wording(CommandOutcome.REJECTED, "內容含有不能使用的字元")));

    private CommandViewModel viewModel;

    public void present(CommandResponse response) {
        String createdId = response.createdId() == null ? "" : response.createdId();
        if (response.succeeded()) {
            viewModel = new CommandViewModel(CommandOutcome.SUCCEEDED, "", createdId);
            return;
        }
        Wording wording = WORDINGS.getOrDefault(response.failureCode(),
                new Wording(CommandOutcome.REJECTED, INVALID_REQUEST_MESSAGE));
        viewModel = new CommandViewModel(wording.outcome(), wording.message(), createdId);
    }

    public CommandViewModel viewModel() {
        if (viewModel == null) {
            throw new IllegalStateException("Use Case 還沒有交出結果");
        }
        return viewModel;
    }
}
