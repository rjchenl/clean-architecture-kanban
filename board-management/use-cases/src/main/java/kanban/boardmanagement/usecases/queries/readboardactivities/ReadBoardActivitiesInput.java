package kanban.boardmanagement.usecases.queries.readboardactivities;

/** 讀取看板動態：讀出某個位置之後的看板動態（其他業務情境的防腐層使用）。 */
public interface ReadBoardActivitiesInput {

    void execute(ReadBoardActivitiesRequest request);
}
