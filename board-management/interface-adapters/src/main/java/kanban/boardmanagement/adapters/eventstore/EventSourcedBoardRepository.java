package kanban.boardmanagement.adapters.eventstore;

import java.util.List;
import java.util.Optional;
import kanban.boardmanagement.entities.board.Board;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.EventStore;

/** 看板 Repository 的實作：載入＝重播事件，儲存＝帶著預期版本號寫入新事件。沒有任何「現況」資料表（ES-1）。 */
public final class EventSourcedBoardRepository implements BoardRepository {

    private final EventStore store;

    public EventSourcedBoardRepository(EventStore store) {
        this.store = store;
    }

    @Override
    public Optional<Board> load(BoardId id) {
        List<BoardEvent> history = store.readStream(streamIdOf(id));
        return history.isEmpty() ? Optional.empty() : Optional.of(Board.rehydrate(history));
    }

    @Override
    public void save(Board board) {
        List<BoardEvent> events = board.uncommittedEvents();
        if (events.isEmpty()) {
            return;
        }
        store.append(streamIdOf(board.id()), board.persistedVersion(), events);
        board.markCommitted();
    }

    private static String streamIdOf(BoardId id) {
        return "board-" + id.value();
    }
}
