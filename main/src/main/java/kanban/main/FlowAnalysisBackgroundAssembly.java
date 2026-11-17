package kanban.main;

import kanban.flowanalysis.adapters.boardactivity.BoardActivitySync;
import kanban.flowanalysis.adapters.controllers.CheckStuckWorkItemsController;
import kanban.flowanalysis.adapters.controllers.ListBoardFlowsController;
import kanban.flowanalysis.adapters.presenters.BoardFlowListPresenter;
import kanban.flowanalysis.adapters.presenters.BoardFlowListViewModel;
import kanban.flowanalysis.adapters.presenters.CheckStuckWorkItemsPresenter;
import kanban.flowanalysis.adapters.presenters.CommandViewModel;
import kanban.flowanalysis.frameworks.scheduling.FlowAnalysisBackgroundWork;
import kanban.flowanalysis.usecases.commands.checkstuckworkitems.impl.CheckStuckWorkItemsUseCase;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.TimeSource;
import kanban.flowanalysis.usecases.queries.listboardflows.BoardFlowListReader;
import kanban.flowanalysis.usecases.queries.listboardflows.impl.ListBoardFlowsUseCase;

/** 流程分析背景工作的組裝程式（CA-8）：排程只認識 FlowAnalysisBackgroundWork，這裡接上防腐層與 Use Case。 */
final class FlowAnalysisBackgroundAssembly implements FlowAnalysisBackgroundWork {

    private final BoardActivitySync sync;
    private final BoardFlowRepository flows;
    private final BoardFlowListReader boardFlows;
    private final TimeSource time;

    FlowAnalysisBackgroundAssembly(BoardActivitySync sync, BoardFlowRepository flows, BoardFlowListReader boardFlows,
                                   TimeSource time) {
        this.sync = sync;
        this.flows = flows;
        this.boardFlows = boardFlows;
        this.time = time;
    }

    @Override
    public void syncBoardActivities() {
        sync.syncOnce();
    }

    @Override
    public BoardFlowListViewModel listBoardFlows() {
        BoardFlowListPresenter presenter = new BoardFlowListPresenter();
        new ListBoardFlowsController(new ListBoardFlowsUseCase(boardFlows, presenter)).handle();
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel checkStuckWorkItems(String boardFlowId) {
        CheckStuckWorkItemsPresenter presenter = new CheckStuckWorkItemsPresenter();
        new CheckStuckWorkItemsController(new CheckStuckWorkItemsUseCase(flows, time, presenter)).handle(boardFlowId);
        return presenter.viewModel();
    }
}
