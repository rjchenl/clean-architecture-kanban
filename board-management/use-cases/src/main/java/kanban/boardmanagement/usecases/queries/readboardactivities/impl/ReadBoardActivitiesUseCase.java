package kanban.boardmanagement.usecases.queries.readboardactivities.impl;

import java.util.List;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityReader;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesInput;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesOutput;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesRequest;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesResponse;

/** 查詢：只讀看板動態紀錄這份讀取模型，不載入聚合、不讀事件紀錄（CQRS-3）。 */
public final class ReadBoardActivitiesUseCase implements ReadBoardActivitiesInput {

    private static final int MAX_COUNT = 500;
    private static final ReadBoardActivitiesResponse NOT_ACCEPTED = new ReadBoardActivitiesResponse(false, List.of());

    private final BoardActivityReader activities;
    private final ReadBoardActivitiesOutput output;

    public ReadBoardActivitiesUseCase(BoardActivityReader activities, ReadBoardActivitiesOutput output) {
        this.activities = activities;
        this.output = output;
    }

    @Override
    public void execute(ReadBoardActivitiesRequest request) {
        if (request == null || request.afterPosition() == null || request.afterPosition() < 0
                || request.maxCount() == null || request.maxCount() < 1 || request.maxCount() > MAX_COUNT) {
            output.present(NOT_ACCEPTED);
            return;
        }
        output.present(new ReadBoardActivitiesResponse(true,
                activities.activitiesAfter(request.afterPosition(), request.maxCount())));
    }
}
