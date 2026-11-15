package kanban.boardmanagement.adapters.projection;

import java.time.Instant;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.entities.board.CardAdded;
import kanban.boardmanagement.entities.board.CardDescription;
import kanban.boardmanagement.entities.board.CardId;
import kanban.boardmanagement.entities.board.CardMoved;
import kanban.boardmanagement.entities.board.CardTitle;
import kanban.boardmanagement.entities.board.ColumnAdded;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.entities.board.Position;

/** Projection 測試共用的事件。 */
public final class ProjectionEvents {

    public static final Instant T0 = Instant.parse("2026-11-14T01:00:00Z");
    public static final Instant T1 = T0.plusSeconds(60);
    public static final Instant T2 = T0.plusSeconds(120);
    public static final BoardId BOARD = new BoardId("board-1");
    public static final ColumnId TODO = new ColumnId("col-todo");
    public static final ColumnId DOING = new ColumnId("col-doing");
    public static final CardId C1 = new CardId("card-1");
    public static final CardId C2 = new CardId("card-2");

    private ProjectionEvents() {
    }

    public static BoardEvent created() {
        return new BoardCreated(BOARD, new BoardName("產品開發"), T0);
    }

    public static BoardEvent column(ColumnId id, String name) {
        return new ColumnAdded(BOARD, id, new ColumnName(name), T0);
    }

    public static BoardEvent card(CardId id, ColumnId column, String title) {
        return new CardAdded(BOARD, id, column, new CardTitle(title), new CardDescription(""), T0);
    }

    public static BoardEvent moved(CardId id, ColumnId from, ColumnId to, int position, Instant at) {
        return new CardMoved(BOARD, id, from, to, new Position(position), at);
    }
}
