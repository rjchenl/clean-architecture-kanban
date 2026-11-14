package kanban.boardmanagement.usecases.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kanban.boardmanagement.entities.board.Board;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.ConcurrentUpdateException;

/** 測試用的記憶體版看板 Repository：保存事件、含樂觀鎖。Use Case 測試不啟動 Spring（CA-3）。 */
public final class FakeBoardRepository implements BoardRepository {

    private final Map<BoardId, List<BoardEvent>> streams = new HashMap<>();
    private boolean conflictOnNextSave;

    /** 測試準備：這個看板已經發生過這些事件。 */
    public void given(BoardId id, List<BoardEvent> history) {
        streams.put(id, new ArrayList<>(history));
    }

    /** 這個看板目前存了哪些事件。 */
    public List<BoardEvent> eventsOf(BoardId id) {
        return List.copyOf(streams.getOrDefault(id, List.of()));
    }

    /** 模擬「載入之後有別人先存了」。 */
    public void conflictOnNextSave() {
        conflictOnNextSave = true;
    }

    @Override
    public Optional<Board> load(BoardId id) {
        List<BoardEvent> history = streams.get(id);
        return history == null ? Optional.empty() : Optional.of(Board.rehydrate(history));
    }

    @Override
    public void save(Board board) {
        List<BoardEvent> stream = streams.computeIfAbsent(board.id(), id -> new ArrayList<>());
        if (conflictOnNextSave || stream.size() != board.persistedVersion()) {
            conflictOnNextSave = false;
            throw new ConcurrentUpdateException("board-" + board.id().value(), board.persistedVersion());
        }
        stream.addAll(board.uncommittedEvents());
        board.markCommitted();
    }
}
