package kanban.flowanalysis.adapters.presenters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kanban.flowanalysis.usecases.commands.CommandResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 流程分析指令的回覆整理成畫面看得懂的結果與中文訊息（CA-9）。 */
class CommandPresenterTest {

    private static CommandViewModel present(CommandResponse response) {
        CommandPresenter presenter = new CommandPresenter();
        presenter.present(response);
        return presenter.viewModel();
    }

    @Test
    void 成功_沒有訊息() {
        assertThat(present(CommandResponse.success(null))).isEqualTo(new CommandViewModel(CommandOutcome.SUCCEEDED, "", ""));
    }

    @ParameterizedTest
    @CsvSource({
        "BOARD_FLOW_NOT_FOUND, NOT_FOUND, 流程分析還沒準備好，請稍後重新整理",
        "FLOW_COLUMN_NOT_FOUND, NOT_FOUND, 找不到這個欄位",
        "FLOW_COLUMN_REMOVED, REJECTED, 這個欄位已經刪除，不能再改階段",
        "STUCK_THRESHOLD_OUT_OF_RANGE, REJECTED, 卡住門檻要是 1 到 365 天",
        "UNKNOWN_STAGE, REJECTED, 不認得這個階段",
        "INVALID_CHARACTERS, REJECTED, 內容含有不能使用的字元",
        "CONCURRENT_UPDATE, CONFLICT, 流程設定已被更新，請重新整理後再試",
        "INVALID_REQUEST, REJECTED, 請求內容不完整或格式不正確",
        "IDENTIFIER_BLANK, REJECTED, 請求內容不完整或格式不正確",
    })
    void 失敗原因_翻成結果與中文訊息(String failureCode, CommandOutcome outcome, String message) {
        assertThat(present(CommandResponse.failure(failureCode))).isEqualTo(new CommandViewModel(outcome, message, ""));
    }

    @Test
    void 還沒交出結果就要ViewModel_程式錯誤() {
        assertThatThrownBy(() -> new CommandPresenter().viewModel()).isInstanceOf(IllegalStateException.class);
    }
}
