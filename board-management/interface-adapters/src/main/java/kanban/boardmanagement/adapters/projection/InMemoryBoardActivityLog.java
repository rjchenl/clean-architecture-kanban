package kanban.boardmanagement.adapters.projection;

import java.util.List;
import java.util.TreeMap;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;

/** 記憶體版看板動態紀錄。 */
public final class InMemoryBoardActivityLog implements BoardActivityLogStore {

    private TreeMap<Long, BoardActivity> activities = new TreeMap<>();
    private long checkpoint;
    private boolean inTransaction;

    @Override
    public synchronized long checkpoint() {
        return checkpoint;
    }

    @Override
    public synchronized void inExclusiveTransaction(Runnable work) {
        if (inTransaction) {
            work.run();
            return;
        }
        TreeMap<Long, BoardActivity> activitiesBefore = new TreeMap<>(activities);
        long checkpointBefore = checkpoint;
        inTransaction = true;
        try {
            work.run();
        } catch (RuntimeException | Error failure) {
            activities = activitiesBefore;
            checkpoint = checkpointBefore;
            throw failure;
        } finally {
            inTransaction = false;
        }
    }

    @Override
    public synchronized void append(BoardActivity activity) {
        requireTransaction();
        activities.put(activity.position(), activity);
    }

    @Override
    public synchronized void advanceCheckpoint(long position) {
        requireTransaction();
        checkpoint = position;
    }

    @Override
    public synchronized void reset() {
        requireTransaction();
        activities.clear();
        checkpoint = 0;
    }

    @Override
    public synchronized List<BoardActivity> activitiesAfter(long position, int maxCount) {
        return activities.tailMap(position, false).values().stream().limit(maxCount).toList();
    }

    private void requireTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("看板動態紀錄的寫入只能在 inExclusiveTransaction 之內");
        }
    }
}
