package kanban.flowanalysis.adapters.boardactivity;

/**
 * 防腐層已經把看板動態處理到哪個位置。只在一筆動態確實記錄成功後才前進；
 * 在兩步之間當掉時會重送同一筆，由看板流程依 F14 略過。
 */
public interface ActivityPositionStore {

    /** 還沒處理過任何動態時為 0。 */
    long lastPosition();

    /**
     * @throws IllegalArgumentException 新位置比目前的位置小；相等時不變
     */
    void advanceTo(long position);
}
