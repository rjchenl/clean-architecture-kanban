package kanban.boardmanagement.usecases.queries.readboardactivities;

/**
 * @param afterPosition 只要這個位置之後的動態；0 代表從頭
 * @param maxCount      最多幾筆，1～500
 */
public record ReadBoardActivitiesRequest(Long afterPosition, Integer maxCount) {
}
