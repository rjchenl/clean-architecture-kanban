package kanban.boardmanagement.adapters.eventstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import kanban.boardmanagement.entities.board.Board;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BoardName;
import kanban.boardmanagement.entities.board.BusinessRuleViolation;
import kanban.boardmanagement.entities.board.ColumnAdded;
import kanban.boardmanagement.entities.board.ColumnId;
import kanban.boardmanagement.entities.board.ColumnName;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.ConcurrentUpdateException;
import org.junit.jupiter.api.Test;

class EventSourcedBoardRepositoryTest {

    private static final Instant AT = Instant.parse("2026-11-14T01:00:00Z");
    private static final BoardId BOARD = new BoardId("board-1");

    private final InMemoryEventStore store = new InMemoryEventStore();
    private final BoardRepository repository = new EventSourcedBoardRepository(store);

    private Board savedBoardWithTodoColumn() {
        Board board = Board.create(BOARD, new BoardName("產品開發"), AT);
        board.addColumn(new ColumnId("col-todo"), new ColumnName("待辦"), AT);
        repository.save(board);
        return board;
    }

    @Test
    void 存入再載入_行為與原聚合相同() {
        savedBoardWithTodoColumn();

        Board loaded = repository.load(BOARD).orElseThrow();

        // 「待辦」已存在，所以同名欄位會被拒絕——證明狀態是從事件重播回來的
        assertThatThrownBy(() -> loaded.addColumn(new ColumnId("col-x"), new ColumnName("待辦"), AT))
                .isInstanceOf(BusinessRuleViolation.class);
        assertThat(loaded.persistedVersion()).isEqualTo(2);
        assertThat(loaded.uncommittedEvents()).isEmpty();
    }

    @Test
    void 載入不存在的看板_回empty() {
        assertThat(repository.load(new BoardId("no-such-board"))).isEmpty();
    }

    @Test
    void 事件寫進以看板識別碼命名的串流() {
        savedBoardWithTodoColumn();

        assertThat(store.readStream("board-board-1")).hasSize(2);
    }

    @Test
    void 儲存成功後未提交事件被清空_版本前進() {
        Board board = savedBoardWithTodoColumn();

        assertThat(board.uncommittedEvents()).isEmpty();
        assertThat(board.persistedVersion()).isEqualTo(2);
    }

    @Test
    void 同一個聚合可以連續儲存多次() {
        Board board = savedBoardWithTodoColumn();
        board.addColumn(new ColumnId("col-doing"), new ColumnName("進行中"), AT);

        repository.save(board);

        assertThat(store.readStream("board-board-1")).hasSize(3).last()
                .isEqualTo(new ColumnAdded(BOARD, new ColumnId("col-doing"), new ColumnName("進行中"), AT));
    }

    @Test
    void 沒有新事件時儲存_不寫入任何東西() {
        Board board = savedBoardWithTodoColumn();

        repository.save(board);

        assertThat(store.readStream("board-board-1")).hasSize(2);
    }

    @Test
    void 兩個人載入同一版本各自修改_後存的丟ConcurrentUpdateException() {
        savedBoardWithTodoColumn();
        Board first = repository.load(BOARD).orElseThrow();
        Board second = repository.load(BOARD).orElseThrow();
        first.addColumn(new ColumnId("col-a"), new ColumnName("甲"), AT);
        second.addColumn(new ColumnId("col-b"), new ColumnName("乙"), AT);

        repository.save(first);

        assertThatThrownBy(() -> repository.save(second)).isInstanceOf(ConcurrentUpdateException.class);
        assertThat(store.readStream("board-board-1")).hasSize(3);
        assertThat(second.uncommittedEvents()).as("失敗時未提交事件要留著").hasSize(1);
    }
}
