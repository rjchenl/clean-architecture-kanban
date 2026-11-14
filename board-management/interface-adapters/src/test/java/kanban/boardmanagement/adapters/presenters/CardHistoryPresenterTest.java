package kanban.boardmanagement.adapters.presenters;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryEntry;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryEntryKind;
import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryResponse;
import org.junit.jupiter.api.Test;

class CardHistoryPresenterTest {

    private static final Instant AT = Instant.parse("2026-11-14T01:00:00Z");

    private final CardHistoryPresenter presenter = new CardHistoryPresenter(ZoneId.of("Asia/Taipei"));

    private CardHistoryViewModel present(CardHistoryEntry... entries) {
        presenter.present(new ViewCardHistoryResponse(true, new CardHistory("card-1", "設計登入頁", List.of(entries))));
        return presenter.viewModel();
    }

    @Test
    void 新增_顯示新增到哪一欄() {
        assertThat(present(new CardHistoryEntry(CardHistoryEntryKind.ADDED, null, "待辦", AT)).lines())
                .containsExactly(new CardHistoryLineViewModel("2026-11-14 09:00", "新增到「待辦」"));
    }

    @Test
    void 換欄_顯示從哪一欄移到哪一欄() {
        assertThat(present(new CardHistoryEntry(CardHistoryEntryKind.MOVED, "待辦", "進行中", AT)).lines())
                .containsExactly(new CardHistoryLineViewModel("2026-11-14 09:00", "從「待辦」移到「進行中」"));
    }

    @Test
    void 刪除_顯示已刪除() {
        assertThat(present(new CardHistoryEntry(CardHistoryEntryKind.REMOVED, "進行中", null, AT)).lines())
                .containsExactly(new CardHistoryLineViewModel("2026-11-14 09:00", "已刪除"));
    }

    @Test
    void 時間依台北時區顯示_跨日也正確() {
        CardHistoryViewModel viewModel = present(
                new CardHistoryEntry(CardHistoryEntryKind.ADDED, null, "待辦", Instant.parse("2026-11-14T16:30:00Z")));

        assertThat(viewModel.lines().get(0).occurredAt()).isEqualTo("2026-11-15 00:30");
    }

    @Test
    void 時區由外部傳入() {
        CardHistoryPresenter utc = new CardHistoryPresenter(ZoneId.of("UTC"));
        utc.present(new ViewCardHistoryResponse(true, new CardHistory("card-1", "卡片", List.of(
                new CardHistoryEntry(CardHistoryEntryKind.ADDED, null, "待辦", AT)))));

        assertThat(utc.viewModel().lines().get(0).occurredAt()).isEqualTo("2026-11-14 01:00");
    }

    @Test
    void 帶出卡片標題與依序的每一行() {
        CardHistoryViewModel viewModel = present(
                new CardHistoryEntry(CardHistoryEntryKind.ADDED, null, "待辦", AT),
                new CardHistoryEntry(CardHistoryEntryKind.MOVED, "待辦", "進行中", AT.plusSeconds(3600)));

        assertThat(viewModel.found()).isTrue();
        assertThat(viewModel.cardTitle()).isEqualTo("設計登入頁");
        assertThat(viewModel.lines()).extracting(CardHistoryLineViewModel::occurredAt)
                .containsExactly("2026-11-14 09:00", "2026-11-14 10:00");
    }

    @Test
    void 找不到_found為false() {
        presenter.present(new ViewCardHistoryResponse(false, null));

        assertThat(presenter.viewModel()).isEqualTo(new CardHistoryViewModel(false, "", List.of()));
    }
}
