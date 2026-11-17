package kanban.flowanalysis.usecases.commands.recordboardactivity.impl;

import static kanban.flowanalysis.usecases.commands.support.BoardFlowCommand.required;

import kanban.flowanalysis.entities.boardflow.ActivityPosition;
import kanban.flowanalysis.entities.boardflow.BoardFlow;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.FlowBoardName;
import kanban.flowanalysis.entities.boardflow.FlowColumnId;
import kanban.flowanalysis.entities.boardflow.FlowColumnName;
import kanban.flowanalysis.entities.boardflow.FlowColumnPosition;
import kanban.flowanalysis.entities.boardflow.WorkItemId;
import kanban.flowanalysis.entities.boardflow.WorkItemTitle;
import kanban.flowanalysis.usecases.commands.BoardChange;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityInput;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityOutput;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityRequest;
import kanban.flowanalysis.usecases.commands.support.BoardFlowCommand;

/**
 * 記錄看板動態：把一件看板上發生的事交給看板流程（看板流程還不存在、而且這件事是看板建立，就先建立）。
 * 只做協調：把 Request 換成值物件、呼叫看板流程對應的方法（DDD-9）。看板上的事一律接受（F14），
 * 對不上的（程式錯誤）由看板流程丟例外，這裡不吞，讓防腐層停在這一筆。
 */
public final class RecordBoardActivityUseCase implements RecordBoardActivityInput {

    private final BoardFlowRepository flows;
    private final RecordBoardActivityOutput output;

    public RecordBoardActivityUseCase(BoardFlowRepository flows, RecordBoardActivityOutput output) {
        this.flows = flows;
        this.output = output;
    }

    @Override
    public void execute(RecordBoardActivityRequest request) {
        output.present(BoardFlowCommand.onBoardFlow(flows,
                () -> requiredFields(request),
                () -> request.change() == BoardChange.BOARD_FLOW_CREATED
                        ? BoardFlow.create(new BoardFlowId(request.boardFlowId()), position(request),
                                new FlowBoardName(request.boardName()), request.occurredAt())
                        : null,
                flow -> {
                    record(flow, request);
                    return null;
                }));
    }

    /** 每一種變化需要的欄位都要有（見 BoardChange）；回傳看板流程識別碼。 */
    private static String requiredFields(RecordBoardActivityRequest r) {
        String id = r.boardFlowId();
        return switch (r.change()) {
            case BOARD_FLOW_CREATED, BOARD_FLOW_RENAMED -> required(id, r.activityPosition(), r.occurredAt(), r.boardName());
            case BOARD_FLOW_ARCHIVED -> required(id, r.activityPosition(), r.occurredAt());
            case FLOW_COLUMN_ADDED, FLOW_COLUMN_RENAMED ->
                    required(id, r.activityPosition(), r.occurredAt(), r.flowColumnId(), r.flowColumnName());
            case FLOW_COLUMN_MOVED ->
                    required(id, r.activityPosition(), r.occurredAt(), r.flowColumnId(), r.flowColumnPosition());
            case FLOW_COLUMN_REMOVED -> required(id, r.activityPosition(), r.occurredAt(), r.flowColumnId());
            case WORK_ITEM_ADDED ->
                    required(id, r.activityPosition(), r.occurredAt(), r.workItemId(), r.flowColumnId(), r.workItemTitle());
            case WORK_ITEM_TITLE_CHANGED ->
                    required(id, r.activityPosition(), r.occurredAt(), r.workItemId(), r.workItemTitle());
            case WORK_ITEM_MOVED -> required(id, r.activityPosition(), r.occurredAt(), r.workItemId(),
                    r.fromFlowColumnId(), r.toFlowColumnId());
            case WORK_ITEM_REMOVED -> required(id, r.activityPosition(), r.occurredAt(), r.workItemId(), r.flowColumnId());
        };
    }

    private static void record(BoardFlow flow, RecordBoardActivityRequest r) {
        ActivityPosition position = position(r);
        switch (r.change()) {
            case BOARD_FLOW_CREATED -> flow.acknowledgeCreation(position);
            case BOARD_FLOW_RENAMED -> flow.renameBoard(position, new FlowBoardName(r.boardName()), r.occurredAt());
            case BOARD_FLOW_ARCHIVED -> flow.archiveBoard(position, r.occurredAt());
            case FLOW_COLUMN_ADDED -> flow.addColumn(position, new FlowColumnId(r.flowColumnId()),
                    new FlowColumnName(r.flowColumnName()), r.occurredAt());
            case FLOW_COLUMN_RENAMED -> flow.renameColumn(position, new FlowColumnId(r.flowColumnId()),
                    new FlowColumnName(r.flowColumnName()), r.occurredAt());
            case FLOW_COLUMN_MOVED -> flow.moveColumn(position, new FlowColumnId(r.flowColumnId()),
                    new FlowColumnPosition(r.flowColumnPosition()), r.occurredAt());
            case FLOW_COLUMN_REMOVED -> flow.removeColumn(position, new FlowColumnId(r.flowColumnId()), r.occurredAt());
            case WORK_ITEM_ADDED -> flow.addWorkItem(position, new WorkItemId(r.workItemId()),
                    new FlowColumnId(r.flowColumnId()), new WorkItemTitle(r.workItemTitle()), r.occurredAt());
            case WORK_ITEM_TITLE_CHANGED -> flow.changeWorkItemTitle(position, new WorkItemId(r.workItemId()),
                    new WorkItemTitle(r.workItemTitle()), r.occurredAt());
            case WORK_ITEM_MOVED -> flow.moveWorkItem(position, new WorkItemId(r.workItemId()),
                    new FlowColumnId(r.fromFlowColumnId()), new FlowColumnId(r.toFlowColumnId()), r.occurredAt());
            case WORK_ITEM_REMOVED -> flow.removeWorkItem(position, new WorkItemId(r.workItemId()),
                    new FlowColumnId(r.flowColumnId()), r.occurredAt());
        }
    }

    private static ActivityPosition position(RecordBoardActivityRequest r) {
        return new ActivityPosition(r.activityPosition());
    }
}
