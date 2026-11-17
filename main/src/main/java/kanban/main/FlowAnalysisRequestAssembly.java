package kanban.main;

import java.time.ZoneId;
import kanban.flowanalysis.adapters.controllers.RemoveStuckThresholdController;
import kanban.flowanalysis.adapters.controllers.SetColumnStageController;
import kanban.flowanalysis.adapters.controllers.SetStuckThresholdController;
import kanban.flowanalysis.adapters.controllers.ViewFlowMetricsController;
import kanban.flowanalysis.adapters.presenters.CommandPresenter;
import kanban.flowanalysis.adapters.presenters.CommandViewModel;
import kanban.flowanalysis.adapters.presenters.FlowMetricsPresenter;
import kanban.flowanalysis.adapters.presenters.FlowMetricsViewModel;
import kanban.flowanalysis.adapters.presenters.RemoveStuckThresholdPresenter;
import kanban.flowanalysis.adapters.presenters.SetColumnStagePresenter;
import kanban.flowanalysis.adapters.presenters.SetStuckThresholdPresenter;
import kanban.flowanalysis.frameworks.web.FlowAnalysisRequests;
import kanban.flowanalysis.usecases.commands.CommandResponse;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;
import kanban.flowanalysis.usecases.commands.removestuckthreshold.impl.RemoveStuckThresholdUseCase;
import kanban.flowanalysis.usecases.commands.setcolumnstage.impl.SetColumnStageUseCase;
import kanban.flowanalysis.usecases.commands.setstuckthreshold.impl.SetStuckThresholdUseCase;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.FlowMetricsReader;
import kanban.flowanalysis.usecases.queries.viewflowmetrics.impl.ViewFlowMetricsUseCase;

/**
 * 流程分析每個 HTTP 請求的組裝程式（CA-8）：只有這裡知道哪個 Controller 接哪個 Use Case、哪個 Presenter。
 * 每個方法為這一次請求新建一組 Presenter、Use Case、Controller；共用的只有儲存與時間來源。
 */
final class FlowAnalysisRequestAssembly implements FlowAnalysisRequests {

    private final BoardFlowRepository flows;
    private final FlowMetricsReader metrics;
    private final TimeSource time;
    private final ZoneId displayZone;

    FlowAnalysisRequestAssembly(BoardFlowRepository flows, FlowMetricsReader metrics, TimeSource time,
                                ZoneId displayZone) {
        this.flows = flows;
        this.metrics = metrics;
        this.time = time;
        this.displayZone = displayZone;
    }

    /** 「最近 8 週」從現在往回算：現在的時間由流程分析的時間來源提供。 */
    @Override
    public FlowMetricsViewModel viewFlowMetrics(String boardFlowId) {
        FlowMetricsPresenter presenter = new FlowMetricsPresenter(displayZone);
        new ViewFlowMetricsController(new ViewFlowMetricsUseCase(metrics, presenter)).handle(boardFlowId, time.now());
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel setColumnStage(String boardFlowId, String flowColumnId, String stage) {
        SetColumnStagePresenter presenter = new SetColumnStagePresenter();
        new SetColumnStageController(new SetColumnStageUseCase(flows, time, presenter))
                .handle(boardFlowId, flowColumnId, stage);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel setStuckThreshold(String boardFlowId, Integer days) {
        SetStuckThresholdPresenter presenter = new SetStuckThresholdPresenter();
        new SetStuckThresholdController(new SetStuckThresholdUseCase(flows, time, presenter)).handle(boardFlowId, days);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel removeStuckThreshold(String boardFlowId) {
        RemoveStuckThresholdPresenter presenter = new RemoveStuckThresholdPresenter();
        new RemoveStuckThresholdController(new RemoveStuckThresholdUseCase(flows, time, presenter)).handle(boardFlowId);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel invalidRequest() {
        CommandPresenter presenter = new CommandPresenter();
        presenter.present(CommandResponse.failure(CommandResponse.INVALID_REQUEST));
        return presenter.viewModel();
    }
}
