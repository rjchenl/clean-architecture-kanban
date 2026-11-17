package kanban.boardmanagement.usecases.queries.readboardactivities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kanban.boardmanagement.usecases.queries.readboardactivities.impl.ReadBoardActivitiesUseCase;
import org.junit.jupiter.api.Test;

class ReadBoardActivitiesUseCaseTest {

    private static final Instant T0 = Instant.parse("2026-11-16T01:00:00Z");

    private static BoardActivity created(long position) {
        return new BoardActivity(position, BoardActivityKind.BOARD_CREATED, "board-1", T0,
                "產品開發", null, null, null, null, null, null, null);
    }

    private final List<BoardActivity> log = List.of(created(1), created(3), created(7));
    private final List<long[]> asked = new ArrayList<>();
    private final BoardActivityReader reader = (position, maxCount) -> {
        asked.add(new long[] {position, maxCount});
        return log.stream().filter(activity -> activity.position() > position).limit(maxCount).toList();
    };
    private final List<ReadBoardActivitiesResponse> responses = new ArrayList<>();
    private final ReadBoardActivitiesInput useCase = new ReadBoardActivitiesUseCase(reader, responses::add);

    @Test
    void 回傳位置之後的動態() {
        useCase.execute(new ReadBoardActivitiesRequest(1L, 10));

        assertThat(responses).containsExactly(new ReadBoardActivitiesResponse(true, List.of(created(3), created(7))));
    }

    @Test
    void 最多回傳maxCount筆() {
        useCase.execute(new ReadBoardActivitiesRequest(0L, 2));

        assertThat(responses.getFirst().activities()).extracting(BoardActivity::position).containsExactly(1L, 3L);
    }

    @Test
    void afterPosition為null或負數_不受理() {
        useCase.execute(new ReadBoardActivitiesRequest(null, 10));
        useCase.execute(new ReadBoardActivitiesRequest(-1L, 10));

        assertThat(responses).containsOnly(new ReadBoardActivitiesResponse(false, List.of())).hasSize(2);
        assertThat(asked).isEmpty();
    }

    @Test
    void maxCount不在1到500之間_不受理() {
        useCase.execute(new ReadBoardActivitiesRequest(0L, null));
        useCase.execute(new ReadBoardActivitiesRequest(0L, 0));
        useCase.execute(new ReadBoardActivitiesRequest(0L, 501));

        assertThat(responses).containsOnly(new ReadBoardActivitiesResponse(false, List.of())).hasSize(3);
        assertThat(asked).isEmpty();
    }

    @Test
    void Request本身為null_不受理() {
        useCase.execute(null);

        assertThat(responses).containsExactly(new ReadBoardActivitiesResponse(false, List.of()));
    }
}
