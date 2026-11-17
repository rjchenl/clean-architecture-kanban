package kanban.flowanalysis.adapters.boardactivity;

/** 記憶體版的讀取位置。重啟後歸 0，從頭重讀，由看板流程依 F14 略過已處理的。 */
public final class InMemoryActivityPositionStore implements ActivityPositionStore {

    private long position;

    @Override
    public synchronized long lastPosition() {
        return position;
    }

    @Override
    public synchronized void advanceTo(long newPosition) {
        if (newPosition < position) {
            throw new IllegalArgumentException("讀取位置不能倒退：目前 " + position + "，要求 " + newPosition);
        }
        position = newPosition;
    }
}
