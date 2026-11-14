package kanban.fixtures.statetable.usecases.commands.gateway;

/** 違規樣本（ES-1）：指令端多了一個存「看板現況」的 Gateway。 */
public interface BoardStateTable {
    void saveCurrentState(String boardId, String stateJson);
}
