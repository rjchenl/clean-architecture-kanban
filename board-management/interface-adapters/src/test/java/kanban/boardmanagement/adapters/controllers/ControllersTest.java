package kanban.boardmanagement.adapters.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import kanban.boardmanagement.usecases.commands.addcard.AddCardRequest;
import kanban.boardmanagement.usecases.commands.addcolumn.AddColumnRequest;
import kanban.boardmanagement.usecases.commands.archiveboard.ArchiveBoardRequest;
import kanban.boardmanagement.usecases.commands.changecardcontent.ChangeCardContentRequest;
import kanban.boardmanagement.usecases.commands.createboard.CreateBoardRequest;
import kanban.boardmanagement.usecases.commands.movecard.MoveCardRequest;
import kanban.boardmanagement.usecases.commands.movecolumn.MoveColumnRequest;
import kanban.boardmanagement.usecases.commands.removecard.RemoveCardRequest;
import kanban.boardmanagement.usecases.commands.removecolumn.RemoveColumnRequest;
import kanban.boardmanagement.usecases.commands.removecolumnwiplimit.RemoveColumnWipLimitRequest;
import kanban.boardmanagement.usecases.commands.renameboard.RenameBoardRequest;
import kanban.boardmanagement.usecases.commands.renamecolumn.RenameColumnRequest;
import kanban.boardmanagement.usecases.commands.repositioncard.RepositionCardRequest;
import kanban.boardmanagement.usecases.commands.setcolumnwiplimit.SetColumnWipLimitRequest;
import kanban.boardmanagement.usecases.queries.viewboard.ViewBoardRequest;
import kanban.boardmanagement.usecases.queries.viewcardhistory.ViewCardHistoryRequest;
import org.junit.jupiter.api.Test;

/** Controller 只做一件事：把原始輸入組成 Request Model，交給 Input Boundary。 */
class ControllersTest {

    @Test
    void CreateBoard_把參數原樣組成Request交給Input() {
        List<CreateBoardRequest> received = new ArrayList<>();

        new CreateBoardController(received::add).handle("新看板");

        assertThat(received).containsExactly(new CreateBoardRequest("新看板"));
    }

    @Test
    void RenameBoard_把參數原樣組成Request交給Input() {
        List<RenameBoardRequest> received = new ArrayList<>();

        new RenameBoardController(received::add).handle("board-1", "新名稱");

        assertThat(received).containsExactly(new RenameBoardRequest("board-1", "新名稱"));
    }

    @Test
    void ArchiveBoard_把參數原樣組成Request交給Input() {
        List<ArchiveBoardRequest> received = new ArrayList<>();

        new ArchiveBoardController(received::add).handle("board-1");

        assertThat(received).containsExactly(new ArchiveBoardRequest("board-1"));
    }

    @Test
    void AddColumn_把參數原樣組成Request交給Input() {
        List<AddColumnRequest> received = new ArrayList<>();

        new AddColumnController(received::add).handle("board-1", "完成");

        assertThat(received).containsExactly(new AddColumnRequest("board-1", "完成"));
    }

    @Test
    void RenameColumn_把參數原樣組成Request交給Input() {
        List<RenameColumnRequest> received = new ArrayList<>();

        new RenameColumnController(received::add).handle("board-1", "col-todo", "準備中");

        assertThat(received).containsExactly(new RenameColumnRequest("board-1", "col-todo", "準備中"));
    }

    @Test
    void MoveColumn_把參數原樣組成Request交給Input() {
        List<MoveColumnRequest> received = new ArrayList<>();

        new MoveColumnController(received::add).handle("board-1", "col-todo", 1);

        assertThat(received).containsExactly(new MoveColumnRequest("board-1", "col-todo", 1));
    }

    @Test
    void MoveColumn_數字參數沒給_原樣交給UseCase判斷() {
        List<MoveColumnRequest> received = new ArrayList<>();

        new MoveColumnController(received::add).handle("x", "x", null);

        assertThat(received).containsExactly(new MoveColumnRequest("x", "x", null));
    }

    @Test
    void SetColumnWipLimit_把參數原樣組成Request交給Input() {
        List<SetColumnWipLimitRequest> received = new ArrayList<>();

        new SetColumnWipLimitController(received::add).handle("board-1", "col-todo", 3);

        assertThat(received).containsExactly(new SetColumnWipLimitRequest("board-1", "col-todo", 3));
    }

    @Test
    void SetColumnWipLimit_數字參數沒給_原樣交給UseCase判斷() {
        List<SetColumnWipLimitRequest> received = new ArrayList<>();

        new SetColumnWipLimitController(received::add).handle("x", "x", null);

        assertThat(received).containsExactly(new SetColumnWipLimitRequest("x", "x", null));
    }

    @Test
    void RemoveColumnWipLimit_把參數原樣組成Request交給Input() {
        List<RemoveColumnWipLimitRequest> received = new ArrayList<>();

        new RemoveColumnWipLimitController(received::add).handle("board-1", "col-doing");

        assertThat(received).containsExactly(new RemoveColumnWipLimitRequest("board-1", "col-doing"));
    }

    @Test
    void RemoveColumn_把參數原樣組成Request交給Input() {
        List<RemoveColumnRequest> received = new ArrayList<>();

        new RemoveColumnController(received::add).handle("board-1", "col-done");

        assertThat(received).containsExactly(new RemoveColumnRequest("board-1", "col-done"));
    }

    @Test
    void AddCard_把參數原樣組成Request交給Input() {
        List<AddCardRequest> received = new ArrayList<>();

        new AddCardController(received::add).handle("board-1", "col-todo", "新卡片", "說明");

        assertThat(received).containsExactly(new AddCardRequest("board-1", "col-todo", "新卡片", "說明"));
    }

    @Test
    void ChangeCardContent_把參數原樣組成Request交給Input() {
        List<ChangeCardContentRequest> received = new ArrayList<>();

        new ChangeCardContentController(received::add).handle("board-1", "card-1", "新標題", "新說明");

        assertThat(received).containsExactly(new ChangeCardContentRequest("board-1", "card-1", "新標題", "新說明"));
    }

    @Test
    void MoveCard_把參數原樣組成Request交給Input() {
        List<MoveCardRequest> received = new ArrayList<>();

        new MoveCardController(received::add).handle("board-1", "card-2", "col-todo", 0);

        assertThat(received).containsExactly(new MoveCardRequest("board-1", "card-2", "col-todo", 0));
    }

    @Test
    void MoveCard_數字參數沒給_原樣交給UseCase判斷() {
        List<MoveCardRequest> received = new ArrayList<>();

        new MoveCardController(received::add).handle("x", "x", "x", null);

        assertThat(received).containsExactly(new MoveCardRequest("x", "x", "x", null));
    }

    @Test
    void RepositionCard_把參數原樣組成Request交給Input() {
        List<RepositionCardRequest> received = new ArrayList<>();

        new RepositionCardController(received::add).handle("board-1", "card-3", 0);

        assertThat(received).containsExactly(new RepositionCardRequest("board-1", "card-3", 0));
    }

    @Test
    void RepositionCard_數字參數沒給_原樣交給UseCase判斷() {
        List<RepositionCardRequest> received = new ArrayList<>();

        new RepositionCardController(received::add).handle("x", "x", null);

        assertThat(received).containsExactly(new RepositionCardRequest("x", "x", null));
    }

    @Test
    void RemoveCard_把參數原樣組成Request交給Input() {
        List<RemoveCardRequest> received = new ArrayList<>();

        new RemoveCardController(received::add).handle("board-1", "card-1");

        assertThat(received).containsExactly(new RemoveCardRequest("board-1", "card-1"));
    }

    @Test
    void ViewBoard_把參數原樣組成Request交給Input() {
        List<ViewBoardRequest> received = new ArrayList<>();

        new ViewBoardController(received::add).handle("board-1");

        assertThat(received).containsExactly(new ViewBoardRequest("board-1"));
    }

    @Test
    void ListBoards_呼叫Input一次() {
        List<String> calls = new ArrayList<>();

        new ListBoardsController(() -> calls.add("called")).handle();

        assertThat(calls).containsExactly("called");
    }

    @Test
    void ViewCardHistory_把參數原樣組成Request交給Input() {
        List<ViewCardHistoryRequest> received = new ArrayList<>();

        new ViewCardHistoryController(received::add).handle("board-1", "card-1");

        assertThat(received).containsExactly(new ViewCardHistoryRequest("board-1", "card-1"));
    }
}
