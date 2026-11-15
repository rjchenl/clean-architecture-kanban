package kanban.boardmanagement.frameworks.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kanban.boardmanagement.adapters.presenters.BoardViewModel;
import kanban.boardmanagement.adapters.presenters.CardHistoryViewModel;
import kanban.boardmanagement.adapters.presenters.CommandOutcome;
import kanban.boardmanagement.adapters.presenters.CommandViewModel;
import org.junit.jupiter.api.Test;

class HttpStatusMappingTest {

    @Test
    void 指令結果一對一換成狀態碼() {
        assertThat(HttpStatusMapping.statusOf(CommandOutcome.SUCCEEDED)).isEqualTo(200);
        assertThat(HttpStatusMapping.statusOf(CommandOutcome.REJECTED)).isEqualTo(422);
        assertThat(HttpStatusMapping.statusOf(CommandOutcome.NOT_FOUND)).isEqualTo(404);
        assertThat(HttpStatusMapping.statusOf(CommandOutcome.CONFLICT)).isEqualTo(409);
    }

    @Test
    void 回覆內容就是ViewModel本身() {
        CommandViewModel viewModel = new CommandViewModel(CommandOutcome.CONFLICT, "看板已被更新，請重新整理後再試", "");

        assertThat(HttpStatusMapping.respond(viewModel).getBody()).isSameAs(viewModel);
        assertThat(HttpStatusMapping.respond(viewModel).getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void 查詢找不到_404() {
        assertThat(HttpStatusMapping.respond(new BoardViewModel(false, "", "", false, "", List.of()))
                .getStatusCode().value()).isEqualTo(404);
        assertThat(HttpStatusMapping.respond(new BoardViewModel(true, "b", "看板", false, "", List.of()))
                .getStatusCode().value()).isEqualTo(200);
        assertThat(HttpStatusMapping.respond(new CardHistoryViewModel(false, "", List.of()))
                .getStatusCode().value()).isEqualTo(404);
    }
}
