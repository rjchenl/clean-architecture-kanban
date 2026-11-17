package kanban.flowanalysis.adapters.presenters;

import java.util.Map;
import kanban.flowanalysis.usecases.commands.CommandResponse;

/**
 * 流程分析所有指令 Presenter 共用的整理方式：把指令的 Response Model 整理成 ViewModel（CA-9）。
 * 所有給使用者看的文字都在這裡；這裡不判斷指令能不能做，只把看板流程給的結果翻成訊息。
 * 和看板管理的 CommandPresenter 分開：兩個情境的訊息各自演變。
 */
public class CommandPresenter {

    private static final String INVALID_REQUEST_MESSAGE = "請求內容不完整或格式不正確";

    private record Wording(CommandOutcome outcome, String message) {
    }

    private static final Map<String, Wording> WORDINGS = Map.ofEntries(
            Map.entry("BOARD_FLOW_NOT_FOUND", new Wording(CommandOutcome.NOT_FOUND, "流程分析還沒準備好，請稍後重新整理")),
            Map.entry("FLOW_COLUMN_NOT_FOUND", new Wording(CommandOutcome.NOT_FOUND, "找不到這個欄位")),
            Map.entry("FLOW_COLUMN_REMOVED", new Wording(CommandOutcome.REJECTED, "這個欄位已經刪除，不能再改階段")),
            Map.entry("STUCK_THRESHOLD_OUT_OF_RANGE", new Wording(CommandOutcome.REJECTED, "卡住門檻要是 1 到 365 天")),
            Map.entry("UNKNOWN_STAGE", new Wording(CommandOutcome.REJECTED, "不認得這個階段")),
            Map.entry("INVALID_CHARACTERS", new Wording(CommandOutcome.REJECTED, "內容含有不能使用的字元")),
            Map.entry("CONCURRENT_UPDATE", new Wording(CommandOutcome.CONFLICT, "流程設定已被更新，請重新整理後再試")));

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
