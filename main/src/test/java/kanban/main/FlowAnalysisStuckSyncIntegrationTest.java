package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kanban.boardmanagement.adapters.projection.BoardActivityLogStore;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind;
import kanban.flowanalysis.adapters.boardactivity.ActivityPositionStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Review Focus 1：看板動態引用了流程分析不認得的工作項目（只有程式錯誤才會發生）。
 * 同步會永久停住，所以在獨立的記憶體模式 context 跑、跑完就丟掉，不碰共用的 PostgreSQL。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "kanban.storage=memory")
@DirtiesContext
class FlowAnalysisStuckSyncIntegrationTest extends FlowAnalysisTestSupport {

    @MockitoSpyBean
    BoardActivityLogStore activityLog;

    @Autowired
    ActivityPositionStore positions;

    @Test
    @SuppressWarnings("unchecked")
    void 動態引用不認得的工作項目_同步停在那一筆_之前的數字照常_看板管理照常() {
        String board = createBoard("停住");
        String todo = addColumn(board, "待辦");
        addCard(board, todo, "串接金流");
        sync();
        long lastReal = activityLog.activitiesAfter(0, Integer.MAX_VALUE).getLast().position();
        long broken = lastReal + 1;
        BoardActivity unknownWorkItem = new BoardActivity(broken, BoardActivityKind.CARD_MOVED, board, Instant.now(),
                null, null, null, null, "card-does-not-exist", null, todo, todo);
        // 讀取時在真實結果後面多一筆引用不存在卡片的動態；之後的真實動態排在它後面，同步過不去就讀不到
        doAnswer(invocation -> {
            long after = invocation.getArgument(0);
            List<BoardActivity> activities = new ArrayList<>(((List<BoardActivity>) invocation.callRealMethod()).stream()
                    .filter(activity -> activity.position() < broken).toList());
            if (after < broken) {
                activities.add(unknownWorkItem);
            }
            return activities;
        }).when(activityLog).activitiesAfter(anyLong(), anyInt());

        sync();
        Reply added = post("/api/boards/" + board + "/cards",
                "{\"columnId\":\"" + todo + "\",\"title\":\"寫文件\",\"description\":\"\"}");
        sync();

        assertThat(positions.lastPosition()).as("停在那一筆之前，沒有默默略過").isEqualTo(lastReal);
        assertThat(columns(viewFlow(board))).as("之前的數字照常顯示").containsExactly("待辦:1");
        assertThat(added.status()).as("看板管理照常運作").isEqualTo(200);
        assertThat(get("/api/boards/" + board).status()).isEqualTo(200);
    }
}
