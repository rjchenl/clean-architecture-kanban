package kanban.boardmanagement.usecases.queries.readboardactivities;

import java.util.List;

/** 讀看板動態紀錄（讀取模型，CQRS-3）。 */
public interface BoardActivityReader {

    /**
     * @param position 只回傳位置大於它的動態；傳 0 代表從頭
     * @param maxCount 最多回傳幾筆
     * @return 依位置遞增排列
     */
    List<BoardActivity> activitiesAfter(long position, int maxCount);
}
