package kanban.flowanalysis.adapters.boardactivity;

import java.util.List;
import java.util.function.Function;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesInput;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesOutput;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesRequest;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesResponse;
import kanban.flowanalysis.usecases.commands.CommandResponse;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityInput;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityOutput;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityRequest;

/**
 * 防腐層的同步（設計第 7.1、7.5 節）：從上次的讀取位置之後讀看板動態，逐筆翻成流程分析的用語、交給「記錄看板動態」，
 * 確認成功才前進讀取位置。
 *
 * <ul>
 *   <li>版本衝突（使用者剛好同時改設定）：立即重試，最多 3 次。</li>
 *   <li>其他失敗或程式錯誤：寫日誌、停在這一筆，下一輪再試。看板管理完全不受影響。</li>
 *   <li>交給流程分析之後、前進讀取位置之前當掉：下一輪重送同一筆，由看板流程依 F14 略過。</li>
 * </ul>
 *
 * <p>只認識兩邊的邊界（Input／Output），不認識 Use Case 的實作：由 Main 傳入建立 Input 的方法（CA-8、DIP）。
 */
public final class BoardActivitySync {

    private static final System.Logger LOG = System.getLogger(BoardActivitySync.class.getName());
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 3;

    private final Function<ReadBoardActivitiesOutput, ReadBoardActivitiesInput> readActivities;
    private final Function<RecordBoardActivityOutput, RecordBoardActivityInput> recordActivity;
    private final ActivityPositionStore positions;

    public BoardActivitySync(Function<ReadBoardActivitiesOutput, ReadBoardActivitiesInput> readActivities,
            Function<RecordBoardActivityOutput, RecordBoardActivityInput> recordActivity,
            ActivityPositionStore positions) {
        this.readActivities = readActivities;
        this.recordActivity = recordActivity;
        this.positions = positions;
    }

    /** 同步到最新；遇到記錄不了的那一筆就停住，下一輪再從那一筆開始。 */
    public void syncOnce() {
        while (true) {
            List<BoardActivity> batch = read(positions.lastPosition());
            if (batch.isEmpty()) {
                return;
            }
            for (BoardActivity activity : batch) {
                if (!record(activity)) {
                    return;
                }
                positions.advanceTo(activity.position());
            }
        }
    }

    private List<BoardActivity> read(long afterPosition) {
        ActivitiesReceived received = new ActivitiesReceived();
        readActivities.apply(received).execute(new ReadBoardActivitiesRequest(afterPosition, BATCH_SIZE));
        if (received.response == null || !received.response.accepted()) {
            throw new IllegalStateException("看板管理不受理讀取看板動態的請求（位置 " + afterPosition + "）");
        }
        return received.response.activities();
    }

    /** @return 是否記錄成功（重送而被略過也算成功） */
    private boolean record(BoardActivity activity) {
        RecordBoardActivityRequest request = BoardActivityTranslation.toRequest(activity);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            RecordingOutcome outcome = new RecordingOutcome();
            try {
                recordActivity.apply(outcome).execute(request);
            } catch (RuntimeException failure) {
                LOG.log(System.Logger.Level.ERROR, "流程分析記錄不了位置 " + activity.position()
                        + " 的看板動態，同步停在這一筆；流程數字停在之前的狀態，看板管理不受影響", failure);
                return false;
            }
            CommandResponse response = outcome.response;
            if (response != null && response.succeeded()) {
                return true;
            }
            boolean conflict = response != null && CommandResponse.CONCURRENT_UPDATE.equals(response.failureCode());
            if (!conflict) {
                LOG.log(System.Logger.Level.ERROR, "流程分析拒絕了位置 " + activity.position() + " 的看板動態（"
                        + (response == null ? "沒有回覆" : response.failureCode()) + "），同步停在這一筆");
                return false;
            }
        }
        LOG.log(System.Logger.Level.WARNING, "位置 " + activity.position() + " 的看板動態連續 " + MAX_ATTEMPTS
                + " 次碰到版本衝突，下一輪再試");
        return false;
    }

    /** 收看板管理「讀取看板動態」的回覆。 */
    private static final class ActivitiesReceived implements ReadBoardActivitiesOutput {

        private ReadBoardActivitiesResponse response;

        @Override
        public void present(ReadBoardActivitiesResponse received) {
            response = received;
        }
    }

    /** 收流程分析「記錄看板動態」的回覆。 */
    private static final class RecordingOutcome implements RecordBoardActivityOutput {

        private CommandResponse response;

        @Override
        public void present(CommandResponse received) {
            response = received;
        }
    }
}
