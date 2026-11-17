package kanban.boardmanagement.usecases.queries.readboardactivities;

import java.util.List;

/**
 * @param accepted   請求內容是否有效；無效時清單為空
 * @param activities 依位置遞增排列
 */
public record ReadBoardActivitiesResponse(boolean accepted, List<BoardActivity> activities) {
}
