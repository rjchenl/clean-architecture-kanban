package kanban.flowanalysis.usecases.queries.viewflowmetrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.impl.ViewFlowMetricsUseCase;
import org.junit.jupiter.api.Test;

class ViewFlowMetricsUseCaseTest {

    private static final Instant AS_OF = Instant.parse("2026-11-20T01:00:00Z");
    private static final FlowMetrics METRICS = new FlowMetrics("board-1", "產品開發", false, 5,
            List.of(new ColumnMetrics("col-doing", "進行中", "IN_PROGRESS", 2, 3_600L)),
            List.of(new WeeklyCompletion(Instant.parse("2026-11-16T16:00:00Z"), 1)),
            new Delivery(1, 7_200L, 7_200L),
            List.of(new StuckWorkItem("card-1", "串接金流", "進行中", Instant.parse("2026-11-10T01:00:00Z"))));

    private final List<Object[]> asked = new ArrayList<>();
    private final FlowMetricsReader reader = (boardFlowId, asOf) -> {
        asked.add(new Object[] {boardFlowId, asOf});
        return boardFlowId.equals("board-1") ? Optional.of(METRICS) : Optional.empty();
    };
    private final List<ViewFlowMetricsResponse> responses = new ArrayList<>();
    private final ViewFlowMetricsInput useCase = new ViewFlowMetricsUseCase(reader, responses::add);

    @Test
    void 找得到_原樣交回() {
        useCase.execute(new ViewFlowMetricsRequest("board-1", AS_OF));

        assertThat(responses).containsExactly(new ViewFlowMetricsResponse(true, METRICS));
        assertThat(asked).singleElement().satisfies(args -> assertThat(args).containsExactly("board-1", AS_OF));
    }

    @Test
    void 找不到_found為false() {
        useCase.execute(new ViewFlowMetricsRequest("board-x", AS_OF));

        assertThat(responses).containsExactly(new ViewFlowMetricsResponse(false, null));
    }

    @Test
    void Request或欄位為null_found為false_不讀讀取模型() {
        useCase.execute(null);
        useCase.execute(new ViewFlowMetricsRequest(null, AS_OF));
        useCase.execute(new ViewFlowMetricsRequest("board-1", null));

        assertThat(responses).containsOnly(new ViewFlowMetricsResponse(false, null)).hasSize(3);
        assertThat(asked).isEmpty();
    }
}
