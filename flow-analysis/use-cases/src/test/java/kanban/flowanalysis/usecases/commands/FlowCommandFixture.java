package kanban.flowanalysis.usecases.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kanban.flowanalysis.entities.boardflow.ActivityPosition;
import kanban.flowanalysis.entities.boardflow.BoardFlowCreated;
import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.ColumnStageSet;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import kanban.flowanalysis.entities.boardflow.FlowColumnAdded;
import kanban.flowanalysis.entities.boardflow.FlowColumnId;
import kanban.flowanalysis.entities.boardflow.FlowColumnName;
import kanban.flowanalysis.entities.boardflow.Stage;
import kanban.flowanalysis.entities.boardflow.WorkItemAdded;
import kanban.flowanalysis.entities.boardflow.WorkItemId;
import kanban.flowanalysis.entities.boardflow.WorkItemTitle;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;
import org.junit.jupiter.api.AfterEach;

/**
 * 流程分析指令 Use Case 測試共用的場景。
 *
 * <p>標準看板流程 board-1：「待辦」(col-todo，位置 2)、「進行中」(col-doing，位置 3，進行中階段)，
 * 待辦裡有 card-1（位置 4）。已處理到的看板動態位置是 4。
 */
public abstract class FlowCommandFixture {

    protected static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");
    protected static final Instant T1 = T0.plusSeconds(3_600);
    protected static final Instant NOW = Instant.parse("2026-11-20T01:00:00Z");
    protected static final BoardFlowId FLOW = new BoardFlowId("board-1");
    protected static final FlowColumnId TODO = new FlowColumnId("col-todo");
    protected static final FlowColumnId DOING = new FlowColumnId("col-doing");
    protected static final WorkItemId CARD_1 = new WorkItemId("card-1");

    protected final FakeBoardFlowRepository flows = new FakeBoardFlowRepository();
    protected final TimeSource time = () -> NOW;
    /** Use Case 的回覆。每個指令恰好回覆一次，所以收到回覆＝一個指令結束：在這裡自動檢查 DDD-5。 */
    protected final List<CommandResponse> responses = new ArrayList<>() {
        @Override
        public boolean add(CommandResponse response) {
            flows.commandFinished();
            return super.add(response);
        }
    };

    private int givenCount;

    @AfterEach
    protected void 一個指令只修改一個看板流程() {
        flows.assertAtMostOneFlowSaved();
    }

    protected static ActivityPosition at(long position) {
        return new ActivityPosition(position);
    }

    protected void givenStandardFlow(BoardFlowEvent... more) {
        List<BoardFlowEvent> history = new ArrayList<>(List.of(
                new BoardFlowCreated(FLOW, at(1), new FlowBoardName("產品開發"), T0),
                new FlowColumnAdded(FLOW, at(2), TODO, new FlowColumnName("待辦"), Stage.UNCLASSIFIED, T0),
                new FlowColumnAdded(FLOW, at(3), DOING, new FlowColumnName("進行中"), Stage.UNCLASSIFIED, T0),
                new ColumnStageSet(FLOW, DOING, Stage.IN_PROGRESS, T0),
                new WorkItemAdded(FLOW, at(4), CARD_1, TODO, new WorkItemTitle("串接金流"), T0)));
        history.addAll(List.of(more));
        flows.given(FLOW, history);
        givenCount = history.size();
    }

    protected CommandResponse onlyResponse() {
        assertThat(responses).as("Use Case 必須恰好呼叫 Output Boundary 一次").hasSize(1);
        return responses.get(0);
    }

    /** 標準看板流程在這次指令之後多存了哪些事件。 */
    protected List<BoardFlowEvent> newEvents() {
        List<BoardFlowEvent> all = flows.eventsOf(FLOW);
        return all.subList(givenCount, all.size());
    }

    protected void assertSucceeded(String expectedCreatedId) {
        assertThat(onlyResponse()).isEqualTo(new CommandResponse(true, null, expectedCreatedId));
    }

    protected void assertFailed(String expectedCode) {
        assertThat(onlyResponse()).isEqualTo(new CommandResponse(false, expectedCode, null));
    }
}
