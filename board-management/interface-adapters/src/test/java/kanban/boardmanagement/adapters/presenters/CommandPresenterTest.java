package kanban.boardmanagement.adapters.presenters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import kanban.boardmanagement.entities.board.RejectionReason;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** CA-9：Presenter 把 Response Model 整理成可以直接顯示的 ViewModel。 */
class CommandPresenterTest {

    private final CommandPresenter presenter = new CommandPresenter();

    @Test
    void 成功_沒有訊息() {
        presenter.present(new CommandResponse(true, null, null));

        assertThat(presenter.viewModel()).isEqualTo(new CommandViewModel(CommandOutcome.SUCCEEDED, "", ""));
    }

    @Test
    void 成功且有新識別碼_帶出識別碼() {
        presenter.present(new CommandResponse(true, null, "card-9"));

        assertThat(presenter.viewModel()).isEqualTo(new CommandViewModel(CommandOutcome.SUCCEEDED, "", "card-9"));
    }

    @ParameterizedTest(name = "{0} → {1}：{2}")
    @CsvSource({
            "BOARD_NOT_FOUND,            NOT_FOUND, 找不到這個看板",
            "COLUMN_NOT_FOUND,           NOT_FOUND, 找不到這個欄位",
            "CARD_NOT_FOUND,             NOT_FOUND, 找不到這張卡片",
            "CONCURRENT_UPDATE,          CONFLICT,  看板已被更新，請重新整理後再試",
            "BOARD_ARCHIVED,             REJECTED,  看板已封存，不能再變更",
            "WIP_LIMIT_EXCEEDED,         REJECTED,  這一欄已達在製品上限，放不進去",
            "WIP_LIMIT_BELOW_CARD_COUNT, REJECTED,  上限不能低於欄內現有的卡片數，請先移走卡片",
            "COLUMN_NOT_EMPTY,           REJECTED,  欄內還有卡片，不能刪除",
            "COLUMN_NAME_DUPLICATED,     REJECTED,  這個看板已經有同名的欄位",
            "NAME_BLANK,                 REJECTED,  名稱不可空白",
            "NAME_TOO_LONG,              REJECTED,  名稱最長 100 字",
            "DESCRIPTION_TOO_LONG,       REJECTED,  說明最長 2000 字",
            "WIP_LIMIT_NOT_POSITIVE,     REJECTED,  在製品上限必須是 1 以上的整數",
            "POSITION_OUT_OF_RANGE,      REJECTED,  位置超出範圍",
            "IDENTIFIER_BLANK,           REJECTED,  請求內容不完整或格式不正確",
            "INVALID_REQUEST,            REJECTED,  請求內容不完整或格式不正確",
            "SOMETHING_NEW,              REJECTED,  請求內容不完整或格式不正確",
    })
    void 失敗原因轉成結果與中文訊息(String failureCode, CommandOutcome outcome, String message) {
        presenter.present(new CommandResponse(false, failureCode, null));

        assertThat(presenter.viewModel()).isEqualTo(new CommandViewModel(outcome, message, ""));
    }

    @ParameterizedTest
    @EnumSource(RejectionReason.class)
    void 每一種拒絕原因都有訊息(RejectionReason reason) {
        presenter.present(new CommandResponse(false, reason.name(), null));

        assertThat(presenter.viewModel().message()).isNotBlank();
        assertThat(presenter.viewModel().outcome()).isNotEqualTo(CommandOutcome.SUCCEEDED);
    }

    @Test
    void 還沒收到結果就要ViewModel_丟例外() {
        assertThatThrownBy(presenter::viewModel).isInstanceOf(IllegalStateException.class);
    }
}
