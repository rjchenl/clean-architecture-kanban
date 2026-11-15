package kanban.boardmanagement.usecases.eventfeed;

import java.util.List;

/** 依全域順序讀出全部事件。只給 Projection 用（CA-11 ISP），指令端與查詢端都不使用。 */
public interface EventFeed {

    /**
     * @param position 只回傳位置大於它的事件；位置從 1 起算，傳 0 代表從頭
     * @param maxCount 最多回傳幾筆
     * @return 依位置遞增排列
     */
    List<RecordedEvent> readAfter(long position, int maxCount);
}
