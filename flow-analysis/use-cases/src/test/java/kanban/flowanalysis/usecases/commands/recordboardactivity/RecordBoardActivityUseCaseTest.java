package kanban.flowanalysis.usecases.commands.recordboardactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.stream.Stream;
import kanban.flowanalysis.entities.boardflow.BoardFlowArchived;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.BoardFlowRenamed;
import kanban.flowanalysis.entities.boardflow.DwellTime;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import kanban.flowanalysis.entities.boardflow.FlowColumnAdded;
import kanban.flowanalysis.entities.boardflow.FlowColumnId;
import kanban.flowanalysis.entities.boardflow.FlowColumnMoved;
import kanban.flowanalysis.entities.boardflow.FlowColumnName;
import kanban.flowanalysis.entities.boardflow.FlowColumnPosition;
import kanban.flowanalysis.entities.boardflow.FlowColumnRemoved;
import kanban.flowanalysis.entities.boardflow.FlowColumnRenamed;
import kanban.flowanalysis.entities.boardflow.Stage;
import kanban.flowanalysis.entities.boardflow.WorkItemAdded;
import kanban.flowanalysis.entities.boardflow.WorkItemFlowResultChanged;
import kanban.flowanalysis.entities.boardflow.WorkItemId;
import kanban.flowanalysis.entities.boardflow.WorkItemMoved;
import kanban.flowanalysis.entities.boardflow.WorkItemRemoved;
import kanban.flowanalysis.entities.boardflow.WorkItemTitle;
import kanban.flowanalysis.entities.boardflow.WorkItemTitleChanged;
import kanban.flowanalysis.usecases.commands.BoardChange;
import kanban.flowanalysis.usecases.commands.CommandResponse;
import kanban.flowanalysis.usecases.commands.FlowCommandFixture;
import kanban.flowanalysis.usecases.commands.recordboardactivity.impl.RecordBoardActivityUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RecordBoardActivityUseCaseTest extends FlowCommandFixture {

    private final RecordBoardActivityInput useCase = new RecordBoardActivityUseCase(flows, responses::add);

    /** 只填這一種變化需要的欄位，其餘為 null。 */
    private static RecordBoardActivityRequest request(long position, BoardChange change, String boardName,
            String columnId, String columnName, Integer columnPosition, String workItemId, String workItemTitle,
            String from, String to) {
        return new RecordBoardActivityRequest(position, change, "board-1", T1, boardName, columnId, columnName,
                columnPosition, workItemId, workItemTitle, from, to);
    }

    static Stream<Arguments> changes() {
        FlowColumnId done = new FlowColumnId("col-done");
        WorkItemId card2 = new WorkItemId("card-2");
        return Stream.of(
                Arguments.of(request(5, BoardChange.BOARD_FLOW_RENAMED, "新名稱", null, null, null, null, null, null, null),
                        new BoardFlowRenamed(FLOW, at(5), new FlowBoardName("新名稱"), T1)),
                Arguments.of(request(5, BoardChange.BOARD_FLOW_ARCHIVED, null, null, null, null, null, null, null, null),
                        new BoardFlowArchived(FLOW, at(5), T1)),
                Arguments.of(request(5, BoardChange.FLOW_COLUMN_ADDED, null, "col-done", "完成", null, null, null, null, null),
                        new FlowColumnAdded(FLOW, at(5), done, new FlowColumnName("完成"), Stage.UNCLASSIFIED, T1)),
                Arguments.of(request(5, BoardChange.FLOW_COLUMN_RENAMED, null, "col-todo", "Todo", null, null, null, null, null),
                        new FlowColumnRenamed(FLOW, at(5), TODO, new FlowColumnName("Todo"), T1)),
                Arguments.of(request(5, BoardChange.FLOW_COLUMN_MOVED, null, "col-doing", null, 0, null, null, null, null),
                        new FlowColumnMoved(FLOW, at(5), DOING, new FlowColumnPosition(0), T1)),
                Arguments.of(request(5, BoardChange.FLOW_COLUMN_REMOVED, null, "col-doing", null, null, null, null, null, null),
                        new FlowColumnRemoved(FLOW, at(5), DOING, T1)),
                Arguments.of(request(5, BoardChange.WORK_ITEM_ADDED, null, "col-todo", null, null, "card-2", "寫文件", null, null),
                        new WorkItemAdded(FLOW, at(5), card2, TODO, new WorkItemTitle("寫文件"), T1)),
                Arguments.of(request(5, BoardChange.WORK_ITEM_TITLE_CHANGED, null, null, null, null, "card-1", "改過", null, null),
                        new WorkItemTitleChanged(FLOW, at(5), CARD_1, new WorkItemTitle("改過"), T1)),
                Arguments.of(request(5, BoardChange.WORK_ITEM_MOVED, null, null, null, null, "card-1", null, "col-todo", "col-doing"),
                        new WorkItemMoved(FLOW, at(5), CARD_1, TODO, DOING, new DwellTime(Duration.ofHours(1)), T1)),
                Arguments.of(request(5, BoardChange.WORK_ITEM_REMOVED, null, "col-todo", null, null, "card-1", null, null, null),
                        new WorkItemRemoved(FLOW, at(5), CARD_1, TODO, T1)));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("changes")
    void 每一種變化_交給看板流程並存回(RecordBoardActivityRequest request, BoardFlowEvent expectedFact) {
        givenStandardFlow();

        useCase.execute(request);

        assertSucceeded(null);
        assertThat(newEvents()).first().isEqualTo(expectedFact);
    }

    @Test
    void 看板建立而看板流程還不存在_建立看板流程() {
        useCase.execute(request(1, BoardChange.BOARD_FLOW_CREATED, "產品開發", null, null, null, null, null, null, null));

        assertSucceeded("board-1");
        assertThat(flows.eventsOf(FLOW)).containsExactly(
                new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T1));
    }

    @Test
    void 重送建立看板_成功且沒有新事件() {
        givenStandardFlow();

        useCase.execute(request(1, BoardChange.BOARD_FLOW_CREATED, "產品開發", null, null, null, null, null, null, null));

        assertSucceeded(null);
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 進入進行中_判斷結果也一起存回() {
        givenStandardFlow();

        useCase.execute(request(5, BoardChange.WORK_ITEM_MOVED, null, null, null, null, "card-1", null, "col-todo", "col-doing"));

        assertThat(newEvents()).last().isEqualTo(new WorkItemFlowResultChanged(FLOW, CARD_1, T1, null, null, T1));
    }

    @Test
    void 看板流程不存在而且不是建立_回覆BOARD_FLOW_NOT_FOUND() {
        useCase.execute(request(5, BoardChange.BOARD_FLOW_RENAMED, "新名稱", null, null, null, null, null, null, null));

        assertFailed(CommandResponse.BOARD_FLOW_NOT_FOUND);
        assertThat(flows.eventsOf(new BoardFlowId("board-1"))).isEmpty();
    }

    @Test
    void 缺必要欄位_回覆INVALID_REQUEST() {
        givenStandardFlow();

        useCase.execute(request(5, BoardChange.WORK_ITEM_MOVED, null, null, null, null, "card-1", null, "col-todo", null));

        assertFailed(CommandResponse.INVALID_REQUEST);
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 沒有變化種類_回覆INVALID_REQUEST() {
        useCase.execute(request(5, null, null, null, null, null, null, null, null, null));

        assertFailed(CommandResponse.INVALID_REQUEST);
    }

    @Test
    void Request本身為null_回覆INVALID_REQUEST() {
        useCase.execute(null);

        assertFailed(CommandResponse.INVALID_REQUEST);
    }

    @Test
    void 引用不存在的工作項目_例外往外丟() {
        // Review Focus 1：程式錯誤不轉成失敗碼，讓防腐層停在這一筆、寫日誌
        givenStandardFlow();

        assertThatThrownBy(() -> useCase.execute(
                request(5, BoardChange.WORK_ITEM_REMOVED, null, "col-todo", null, null, "card-x", null, null, null)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(newEvents()).isEmpty();
    }

    @Test
    void 儲存時版本衝突_回覆CONCURRENT_UPDATE() {
        givenStandardFlow();
        flows.conflictOnNextSave();

        useCase.execute(request(5, BoardChange.BOARD_FLOW_RENAMED, "新名稱", null, null, null, null, null, null, null));

        assertFailed(CommandResponse.CONCURRENT_UPDATE);
        assertThat(newEvents()).isEmpty();
    }
}
