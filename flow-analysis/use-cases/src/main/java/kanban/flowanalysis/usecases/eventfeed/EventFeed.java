package kanban.flowanalysis.usecases.eventfeed;

import java.util.List;

/**
 * 依寫入順序讀出流程分析的全部事件，只給 Projection 用（CA-11 ISP：和寫入用的 EventStore 分開）。
 */
public interface EventFeed {

    /**
     * @param position 只回傳位置大於它的事件；位置從 1 起算，傳 0 代表從頭
     * @param maxCount 最多回傳幾筆
     * @return 依位置遞增排列
     */
    List<RecordedEvent> readAfter(long position, int maxCount);
}
