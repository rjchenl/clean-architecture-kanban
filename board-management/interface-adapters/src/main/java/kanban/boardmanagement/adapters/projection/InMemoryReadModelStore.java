package kanban.boardmanagement.adapters.projection;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kanban.boardmanagement.usecases.queries.listboards.BoardSummary;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;

/** 記憶體版讀取模型。 */
public final class InMemoryReadModelStore implements ReadModelStore {

    private Map<String, BoardSnapshot> boards = new LinkedHashMap<>();
    private Map<String, CardHistory> histories = new HashMap<>();
    private long checkpoint;
    private boolean inTransaction;

    @Override
    public synchronized long checkpoint() {
        return checkpoint;
    }

    @Override
    public synchronized void inExclusiveTransaction(Runnable work) {
        if (inTransaction) {
            work.run();
            return;
        }
        Map<String, BoardSnapshot> boardsBefore = new LinkedHashMap<>(boards);
        Map<String, CardHistory> historiesBefore = new HashMap<>(histories);
        long checkpointBefore = checkpoint;
        inTransaction = true;
        try {
            work.run();
        } catch (RuntimeException | Error failure) {
            boards = boardsBefore;
            histories = historiesBefore;
            checkpoint = checkpointBefore;
            throw failure;
        } finally {
            inTransaction = false;
        }
    }

    @Override
    public synchronized void saveBoard(BoardSnapshot board) {
        requireTransaction();
        boards.put(board.boardId(), board);
    }

    @Override
    public synchronized void saveCardHistory(String boardId, CardHistory history) {
        requireTransaction();
        histories.put(keyOf(boardId, history.cardId()), history);
    }

    @Override
    public synchronized void advanceCheckpoint(long position) {
        requireTransaction();
        checkpoint = position;
    }

    @Override
    public synchronized void reset() {
        requireTransaction();
        boards.clear();
        histories.clear();
        checkpoint = 0;
    }

    @Override
    public synchronized Optional<BoardSnapshot> boardById(String boardId) {
        return Optional.ofNullable(boards.get(boardId));
    }

    @Override
    public synchronized List<BoardSummary> allBoards() {
        return boards.values().stream()
                .map(board -> new BoardSummary(board.boardId(), board.name(), board.archived()))
                .toList();
    }

    @Override
    public synchronized Optional<CardHistory> cardHistory(String boardId, String cardId) {
        return Optional.ofNullable(histories.get(keyOf(boardId, cardId)));
    }

    private void requireTransaction() {
        if (!inTransaction) {
            throw new IllegalStateException("讀取模型的寫入只能在 inExclusiveTransaction 之內");
        }
    }

    private static String keyOf(String boardId, String cardId) {
        return boardId + "/" + cardId;
    }
}
