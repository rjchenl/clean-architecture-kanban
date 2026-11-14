package kanban.main;

import java.time.ZoneId;
import kanban.boardmanagement.adapters.controllers.AddCardController;
import kanban.boardmanagement.adapters.controllers.AddColumnController;
import kanban.boardmanagement.adapters.controllers.ArchiveBoardController;
import kanban.boardmanagement.adapters.controllers.ChangeCardContentController;
import kanban.boardmanagement.adapters.controllers.CreateBoardController;
import kanban.boardmanagement.adapters.controllers.ListBoardsController;
import kanban.boardmanagement.adapters.controllers.MoveCardController;
import kanban.boardmanagement.adapters.controllers.MoveColumnController;
import kanban.boardmanagement.adapters.controllers.RemoveCardController;
import kanban.boardmanagement.adapters.controllers.RemoveColumnController;
import kanban.boardmanagement.adapters.controllers.RemoveColumnWipLimitController;
import kanban.boardmanagement.adapters.controllers.RenameBoardController;
import kanban.boardmanagement.adapters.controllers.RenameColumnController;
import kanban.boardmanagement.adapters.controllers.RepositionCardController;
import kanban.boardmanagement.adapters.controllers.SetColumnWipLimitController;
import kanban.boardmanagement.adapters.controllers.ViewBoardController;
import kanban.boardmanagement.adapters.controllers.ViewCardHistoryController;
import kanban.boardmanagement.adapters.presenters.BoardListPresenter;
import kanban.boardmanagement.adapters.presenters.BoardListViewModel;
import kanban.boardmanagement.adapters.presenters.BoardPresenter;
import kanban.boardmanagement.adapters.presenters.BoardViewModel;
import kanban.boardmanagement.adapters.presenters.CardHistoryPresenter;
import kanban.boardmanagement.adapters.presenters.CardHistoryViewModel;
import kanban.boardmanagement.adapters.presenters.CommandPresenter;
import kanban.boardmanagement.adapters.presenters.CommandViewModel;
import kanban.boardmanagement.frameworks.web.BoardManagementRequests;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.addcard.impl.AddCardUseCase;
import kanban.boardmanagement.usecases.commands.addcolumn.impl.AddColumnUseCase;
import kanban.boardmanagement.usecases.commands.archiveboard.impl.ArchiveBoardUseCase;
import kanban.boardmanagement.usecases.commands.changecardcontent.impl.ChangeCardContentUseCase;
import kanban.boardmanagement.usecases.commands.createboard.impl.CreateBoardUseCase;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.IdentityGenerator;
import kanban.boardmanagement.usecases.commands.gateway.TimeSource;
import kanban.boardmanagement.usecases.commands.movecard.impl.MoveCardUseCase;
import kanban.boardmanagement.usecases.commands.movecolumn.impl.MoveColumnUseCase;
import kanban.boardmanagement.usecases.commands.removecard.impl.RemoveCardUseCase;
import kanban.boardmanagement.usecases.commands.removecolumn.impl.RemoveColumnUseCase;
import kanban.boardmanagement.usecases.commands.removecolumnwiplimit.impl.RemoveColumnWipLimitUseCase;
import kanban.boardmanagement.usecases.commands.renameboard.impl.RenameBoardUseCase;
import kanban.boardmanagement.usecases.commands.renamecolumn.impl.RenameColumnUseCase;
import kanban.boardmanagement.usecases.commands.repositioncard.impl.RepositionCardUseCase;
import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.impl.SetColumnWipLimitUseCase;
import kanban.boardmanagement.usecases.queries.listboards.BoardSummaryReader;
import kanban.boardmanagement.usecases.queries.listboards.impl.ListBoardsUseCase;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshotReader;
import kanban.boardmanagement.usecases.queries.viewboard.impl.ViewBoardUseCase;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryReader;
import kanban.boardmanagement.usecases.queries.viewcardhistory.impl.ViewCardHistoryUseCase;

/**
 * 每個請求的組裝程式（CA-8）：只有這裡知道哪個 Controller 接哪個 Use Case、哪個 Presenter。
 *
 * <p>每個方法為這一次請求新建一組 Presenter、Use Case、Controller：
 * Use Case 在建立時就拿到它的 Output Boundary（Presenter），Controller 只認識 Input Boundary。
 * 三者都很輕、不共用狀態；共用的只有儲存、時間與識別碼來源。
 */
final class BoardManagementRequestAssembly implements BoardManagementRequests {

    private final BoardRepository boards;
    private final BoardSnapshotReader boardSnapshots;
    private final BoardSummaryReader boardSummaries;
    private final CardHistoryReader cardHistories;
    private final TimeSource time;
    private final IdentityGenerator ids;
    private final ZoneId displayZone;

    BoardManagementRequestAssembly(BoardRepository boards, BoardSnapshotReader boardSnapshots,
                                   BoardSummaryReader boardSummaries, CardHistoryReader cardHistories,
                                   TimeSource time, IdentityGenerator ids, ZoneId displayZone) {
        this.boards = boards;
        this.boardSnapshots = boardSnapshots;
        this.boardSummaries = boardSummaries;
        this.cardHistories = cardHistories;
        this.time = time;
        this.ids = ids;
        this.displayZone = displayZone;
    }

    // ---------------------------------------------------------------- 指令

    @Override
    public CommandViewModel createBoard(String name) {
        CommandPresenter presenter = new CommandPresenter();
        new CreateBoardController(new CreateBoardUseCase(boards, time, ids, presenter)).handle(name);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel renameBoard(String boardId, String name) {
        CommandPresenter presenter = new CommandPresenter();
        new RenameBoardController(new RenameBoardUseCase(boards, time, presenter)).handle(boardId, name);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel archiveBoard(String boardId) {
        CommandPresenter presenter = new CommandPresenter();
        new ArchiveBoardController(new ArchiveBoardUseCase(boards, time, presenter)).handle(boardId);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel addColumn(String boardId, String name) {
        CommandPresenter presenter = new CommandPresenter();
        new AddColumnController(new AddColumnUseCase(boards, time, ids, presenter)).handle(boardId, name);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel renameColumn(String boardId, String columnId, String name) {
        CommandPresenter presenter = new CommandPresenter();
        new RenameColumnController(new RenameColumnUseCase(boards, time, presenter)).handle(boardId, columnId, name);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel moveColumn(String boardId, String columnId, Integer toPosition) {
        CommandPresenter presenter = new CommandPresenter();
        new MoveColumnController(new MoveColumnUseCase(boards, time, presenter)).handle(boardId, columnId, toPosition);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel setColumnWipLimit(String boardId, String columnId, Integer limit) {
        CommandPresenter presenter = new CommandPresenter();
        new SetColumnWipLimitController(new SetColumnWipLimitUseCase(boards, time, presenter)).handle(boardId, columnId, limit);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel removeColumnWipLimit(String boardId, String columnId) {
        CommandPresenter presenter = new CommandPresenter();
        new RemoveColumnWipLimitController(new RemoveColumnWipLimitUseCase(boards, time, presenter)).handle(boardId, columnId);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel removeColumn(String boardId, String columnId) {
        CommandPresenter presenter = new CommandPresenter();
        new RemoveColumnController(new RemoveColumnUseCase(boards, time, presenter)).handle(boardId, columnId);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel addCard(String boardId, String columnId, String title, String description) {
        CommandPresenter presenter = new CommandPresenter();
        new AddCardController(new AddCardUseCase(boards, time, ids, presenter)).handle(boardId, columnId, title, description);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel changeCardContent(String boardId, String cardId, String title, String description) {
        CommandPresenter presenter = new CommandPresenter();
        new ChangeCardContentController(new ChangeCardContentUseCase(boards, time, presenter)).handle(boardId, cardId, title, description);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel moveCard(String boardId, String cardId, String toColumnId, Integer toPosition) {
        CommandPresenter presenter = new CommandPresenter();
        new MoveCardController(new MoveCardUseCase(boards, time, presenter)).handle(boardId, cardId, toColumnId, toPosition);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel repositionCard(String boardId, String cardId, Integer toPosition) {
        CommandPresenter presenter = new CommandPresenter();
        new RepositionCardController(new RepositionCardUseCase(boards, time, presenter)).handle(boardId, cardId, toPosition);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel removeCard(String boardId, String cardId) {
        CommandPresenter presenter = new CommandPresenter();
        new RemoveCardController(new RemoveCardUseCase(boards, time, presenter)).handle(boardId, cardId);
        return presenter.viewModel();
    }

    // ---------------------------------------------------------------- 查詢

    @Override
    public BoardViewModel viewBoard(String boardId) {
        BoardPresenter presenter = new BoardPresenter();
        new ViewBoardController(new ViewBoardUseCase(boardSnapshots, presenter)).handle(boardId);
        return presenter.viewModel();
    }

    @Override
    public BoardListViewModel listBoards() {
        BoardListPresenter presenter = new BoardListPresenter();
        new ListBoardsController(new ListBoardsUseCase(boardSummaries, presenter)).handle();
        return presenter.viewModel();
    }

    @Override
    public CardHistoryViewModel viewCardHistory(String boardId, String cardId) {
        CardHistoryPresenter presenter = new CardHistoryPresenter(displayZone);
        new ViewCardHistoryController(new ViewCardHistoryUseCase(cardHistories, presenter)).handle(boardId, cardId);
        return presenter.viewModel();
    }

    @Override
    public CommandViewModel invalidRequest() {
        CommandPresenter presenter = new CommandPresenter();
        presenter.present(CommandResponse.failure(CommandResponse.INVALID_REQUEST));
        return presenter.viewModel();
    }
}
