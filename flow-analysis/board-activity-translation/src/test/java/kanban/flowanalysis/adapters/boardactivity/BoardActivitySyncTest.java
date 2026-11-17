package kanban.flowanalysis.adapters.boardactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.stream.LongStream;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesInput;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesOutput;
import kanban.boardmanagement.usecases.queries.readboardactivities.ReadBoardActivitiesResponse;
import kanban.flowanalysis.usecases.commands.CommandResponse;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityInput;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityOutput;
import kanban.flowanalysis.usecases.commands.recordboardactivity.RecordBoardActivityRequest;
import org.junit.jupiter.api.Test;

/** 防腐層的同步：讀看板動態 → 翻譯 → 交給流程分析 → 確認成功才前進讀取位置（設計第 7.5 節）。 */
class BoardActivitySyncTest {

    private static final Instant AT = Instant.parse("2026-11-16T01:00:00Z");

    private final List<BoardActivity> published = new ArrayList<>();
    private final List<RecordBoardActivityRequest> recorded = new ArrayList<>();
    /** 每一次記錄要回覆什麼；空了就回覆成功。 */
    private final Deque<Object> replies = new ArrayDeque<>();
    private final InMemoryActivityPositionStore positions = new InMemoryActivityPositionStore();

    private final Function<ReadBoardActivitiesOutput, ReadBoardActivitiesInput> readActivities =
            output -> request -> output.present(new ReadBoardActivitiesResponse(true, published.stream()
                    .filter(activity -> activity.position() > request.afterPosition())
                    .limit(request.maxCount())
                    .toList()));

    private final Function<RecordBoardActivityOutput, RecordBoardActivityInput> recordActivity =
            output -> request -> {
                recorded.add(request);
                Object reply = replies.isEmpty() ? CommandResponse.success(null) : replies.poll();
                if (reply instanceof RuntimeException failure) {
                    throw failure;
                }
                output.present((CommandResponse) reply);
            };

    private final BoardActivitySync sync = new BoardActivitySync(readActivities, recordActivity, positions);

    private static BoardActivity renamed(long position) {
        return new BoardActivity(position, BoardActivityKind.BOARD_RENAMED, "board-1", AT, "名稱" + position,
                null, null, null, null, null, null, null);
    }

    private void publish(long... positions) {
        for (long position : positions) {
            published.add(renamed(position));
        }
    }

    private List<Long> recordedPositions() {
        return recorded.stream().map(RecordBoardActivityRequest::activityPosition).toList();
    }

    @Test
    void 每一筆成功_讀取位置前進到最後一筆() {
        publish(1, 2, 5);

        sync.syncOnce();

        assertThat(recordedPositions()).containsExactly(1L, 2L, 5L);
        assertThat(positions.lastPosition()).isEqualTo(5);
    }

    @Test
    void 沒有新動態_什麼都不做() {
        sync.syncOnce();

        assertThat(recorded).isEmpty();
        assertThat(positions.lastPosition()).isZero();
    }

    @Test
    void 從上次的位置之後接著讀() {
        publish(1, 2, 3);
        positions.advanceTo(2);

        sync.syncOnce();

        assertThat(recordedPositions()).containsExactly(3L);
    }

    @Test
    void 某一筆失敗_停在那一筆_之後的不送_位置不前進() {
        // Review Focus 1：寧可停住讓人發現，也不跳過
        publish(1, 2, 3);
        replies.add(CommandResponse.success(null));
        replies.add(CommandResponse.failure(CommandResponse.BOARD_FLOW_NOT_FOUND));

        sync.syncOnce();

        assertThat(recordedPositions()).containsExactly(1L, 2L);
        assertThat(positions.lastPosition()).isEqualTo(1);
    }

    @Test
    void 記錄時丟例外_停在那一筆_不往外丟() {
        publish(1, 2);
        replies.add(new IllegalStateException("看板流程的紀錄和看板動態對不上"));

        assertThatCode(sync::syncOnce).doesNotThrowAnyException();

        assertThat(recordedPositions()).containsExactly(1L);
        assertThat(positions.lastPosition()).isZero();
    }

    @Test
    void 版本衝突_重試後成功_位置前進() {
        publish(1);
        replies.add(CommandResponse.failure(CommandResponse.CONCURRENT_UPDATE));
        replies.add(CommandResponse.failure(CommandResponse.CONCURRENT_UPDATE));

        sync.syncOnce();

        assertThat(recordedPositions()).containsExactly(1L, 1L, 1L);
        assertThat(positions.lastPosition()).isEqualTo(1);
    }

    @Test
    void 版本衝突連續三次_停在那一筆() {
        publish(1, 2);
        replies.add(CommandResponse.failure(CommandResponse.CONCURRENT_UPDATE));
        replies.add(CommandResponse.failure(CommandResponse.CONCURRENT_UPDATE));
        replies.add(CommandResponse.failure(CommandResponse.CONCURRENT_UPDATE));

        sync.syncOnce();

        assertThat(recordedPositions()).containsExactly(1L, 1L, 1L);
        assertThat(positions.lastPosition()).isZero();
    }

    @Test
    void 超過一批_全部送完() {
        publish(LongStream.rangeClosed(1, 250).toArray());

        sync.syncOnce();

        assertThat(recorded).hasSize(250);
        assertThat(positions.lastPosition()).isEqualTo(250);
    }

    @Test
    void 送出後當掉沒前進位置_下一輪重送同一筆() {
        // 交給流程分析成功之後、前進讀取位置之前當掉：下一輪重送同一筆，由看板流程依 F14 略過
        publish(1);
        ActivityPositionStore crashing = new ActivityPositionStore() {
            private boolean crashed;

            @Override
            public long lastPosition() {
                return positions.lastPosition();
            }

            @Override
            public void advanceTo(long position) {
                if (!crashed) {
                    crashed = true;
                    throw new IllegalStateException("當掉");
                }
                positions.advanceTo(position);
            }
        };
        BoardActivitySync crashingSync = new BoardActivitySync(readActivities, recordActivity, crashing);

        try {
            crashingSync.syncOnce();
        } catch (IllegalStateException expected) {
            // 第一輪當掉
        }
        crashingSync.syncOnce();

        assertThat(recordedPositions()).containsExactly(1L, 1L);
        assertThat(positions.lastPosition()).isEqualTo(1);
    }
}
