package kanban.main;

import static org.assertj.core.api.Assertions.assertThatCode;

import kanban.boardmanagement.adapters.eventstore.UnknownEventTypeException;
import kanban.boardmanagement.adapters.projection.BoardProjection;
import kanban.boardmanagement.adapters.projection.InMemoryReadModelStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * 啟動時讀取模型補不上（例如事件紀錄裡有一筆不認得的事件）不可以讓整個應用程式起不來：
 * 事件紀錄仍然完好，指令照常可用；讀取模型停在出問題的那一筆之前，錯誤會寫進日誌。
 */
class StartupCatchUpFailureTest {

    @Test
    void 啟動時補讀失敗_應用程式照常啟動() {
        BoardProjection stuck = new BoardProjection((position, maxCount) -> {
            throw new UnknownEventTypeException("BoardExploded", 1);
        }, new InMemoryReadModelStore());

        assertThatCode(() -> new BoardManagementWiring().startupCatchUp(stuck).run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }
}
