package kanban.boardmanagement.entities.board;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 看板聚合：一致性邊界（DDD-4）。外部只能呼叫這裡的指令方法。
 *
 * <p>事件溯源寫法（ES-3）：指令方法只做「檢查 → 建立事件 → record」；
 * 狀態只在 apply／on… 方法裡改變，這些方法不檢查、不會失敗、沒有副作用。
 */
public final class Board {

    private BoardId id;
    private BoardName name;
    private boolean archived;
    private List<Column> columns = List.of();

    private long persistedVersion;
    private final List<BoardEvent> pendingEvents = new ArrayList<>();

    private Board() {
    }

    /** 建立看板。新看板沒有任何欄位（R9）。 */
    public static Board create(BoardId id, BoardName name, Instant at) {
        Board board = new Board();
        board.record(new BoardCreated(id, name, at));
        return board;
    }

    /** 載入聚合＝依序重播它的全部事件（ES-4）。 */
    public static Board rehydrate(List<BoardEvent> history) {
        if (history.isEmpty()) {
            throw new IllegalArgumentException("history must not be empty");
        }
        Board board = new Board();
        for (BoardEvent event : history) {
            board.apply(event);
        }
        board.persistedVersion = history.size();
        return board;
    }

    public BoardId id() {
        return id;
    }

    /** 已存入事件紀錄的事件數，寫入時當作預期版本號（ES-6）。 */
    public long persistedVersion() {
        return persistedVersion;
    }

    /** 這次操作新產生、還沒存入的事件。 */
    public List<BoardEvent> uncommittedEvents() {
        return List.copyOf(pendingEvents);
    }

    /** 事件存入成功後呼叫。 */
    public void markCommitted() {
        persistedVersion += pendingEvents.size();
        pendingEvents.clear();
    }

    // ---------------------------------------------------------------- 指令：看板

    public void rename(BoardName newName, Instant at) {
        requireNotArchived();
        if (newName.equals(name)) {
            return;
        }
        record(new BoardRenamed(id, newName, at));
    }

    public void archive(Instant at) {
        requireNotArchived();
        record(new BoardArchived(id, at));
    }

    // ---------------------------------------------------------------- 指令：欄位

    public void addColumn(ColumnId columnId, ColumnName columnName, Instant at) {
        requireNotArchived();
        requireColumnIdUnused(columnId);
        requireColumnNameUnused(columnName);
        record(new ColumnAdded(id, columnId, columnName, at));
    }

    public void renameColumn(ColumnId columnId, ColumnName newName, Instant at) {
        requireNotArchived();
        Column column = requireColumn(columnId);
        if (newName.equals(column.name())) {
            return;
        }
        requireColumnNameUnused(newName);
        record(new ColumnRenamed(id, columnId, newName, at));
    }

    public void moveColumn(ColumnId columnId, Position toPosition, Instant at) {
        requireNotArchived();
        Column column = requireColumn(columnId);
        requireWithin(toPosition, columns.size() - 1);
        if (columns.indexOf(column) == toPosition.index()) {
            return;
        }
        record(new ColumnMoved(id, columnId, toPosition, at));
    }

    public void setColumnWipLimit(ColumnId columnId, WipLimit wipLimit, Instant at) {
        requireNotArchived();
        Column column = requireColumn(columnId);
        if (wipLimit.equals(column.wipLimit())) {
            return;
        }
        if (wipLimit.value() < column.cardCount()) {
            throw new BusinessRuleViolation(RejectionReason.WIP_LIMIT_BELOW_CARD_COUNT);
        }
        record(new ColumnWipLimitSet(id, columnId, wipLimit, at));
    }

    public void removeColumnWipLimit(ColumnId columnId, Instant at) {
        requireNotArchived();
        Column column = requireColumn(columnId);
        if (column.wipLimit() == null) {
            return;
        }
        record(new ColumnWipLimitRemoved(id, columnId, at));
    }

    public void removeColumn(ColumnId columnId, Instant at) {
        requireNotArchived();
        Column column = requireColumn(columnId);
        if (column.cardCount() > 0) {
            throw new BusinessRuleViolation(RejectionReason.COLUMN_NOT_EMPTY);
        }
        record(new ColumnRemoved(id, columnId, at));
    }

    // ---------------------------------------------------------------- 規則檢查

    private void requireNotArchived() {
        if (archived) {
            throw new BusinessRuleViolation(RejectionReason.BOARD_ARCHIVED);
        }
    }

    private Column requireColumn(ColumnId columnId) {
        int index = indexOfColumn(columnId);
        if (index < 0) {
            throw new BusinessRuleViolation(RejectionReason.COLUMN_NOT_FOUND);
        }
        return columns.get(index);
    }

    private void requireColumnIdUnused(ColumnId columnId) {
        if (indexOfColumn(columnId) >= 0) {
            throw new IllegalArgumentException("column id already used: " + columnId.value());
        }
    }

    private void requireColumnNameUnused(ColumnName columnName) {
        for (Column column : columns) {
            if (column.name().equals(columnName)) {
                throw new BusinessRuleViolation(RejectionReason.COLUMN_NAME_DUPLICATED);
            }
        }
    }

    private void requireWithin(Position position, int maxIndex) {
        if (position.index() > maxIndex) {
            throw new BusinessRuleViolation(RejectionReason.POSITION_OUT_OF_RANGE);
        }
    }

    // ---------------------------------------------------------------- 事件記錄與套用

    private void record(BoardEvent event) {
        apply(event);
        pendingEvents.add(event);
    }

    private void apply(BoardEvent event) {
        switch (event) {
            case BoardCreated e -> onBoardCreated(e);
            case BoardRenamed e -> onBoardRenamed(e);
            case BoardArchived e -> onBoardArchived(e);
            case ColumnAdded e -> onColumnAdded(e);
            case ColumnRenamed e -> onColumnRenamed(e);
            case ColumnMoved e -> onColumnMoved(e);
            case ColumnWipLimitSet e -> onColumnWipLimitSet(e);
            case ColumnWipLimitRemoved e -> onColumnWipLimitRemoved(e);
            case ColumnRemoved e -> onColumnRemoved(e);
            default -> {
                // 卡片事件的套用在 Task 5 加入；屆時移除 default，讓編譯器檢查每種事件都有處理
            }
        }
    }

    private void onBoardCreated(BoardCreated event) {
        id = event.boardId();
        name = event.name();
    }

    private void onBoardRenamed(BoardRenamed event) {
        name = event.name();
    }

    private void onBoardArchived(BoardArchived event) {
        archived = true;
    }

    private void onColumnAdded(ColumnAdded event) {
        columns = Ordered.appended(columns, new Column(event.columnId(), event.name(), null, List.of()));
    }

    private void onColumnRenamed(ColumnRenamed event) {
        int index = indexOfColumn(event.columnId());
        columns = Ordered.replaced(columns, index, columns.get(index).withName(event.name()));
    }

    private void onColumnMoved(ColumnMoved event) {
        columns = Ordered.moved(columns, indexOfColumn(event.columnId()), event.toPosition().index());
    }

    private void onColumnWipLimitSet(ColumnWipLimitSet event) {
        int index = indexOfColumn(event.columnId());
        columns = Ordered.replaced(columns, index, columns.get(index).withWipLimit(event.wipLimit()));
    }

    private void onColumnWipLimitRemoved(ColumnWipLimitRemoved event) {
        int index = indexOfColumn(event.columnId());
        columns = Ordered.replaced(columns, index, columns.get(index).withWipLimit(null));
    }

    private void onColumnRemoved(ColumnRemoved event) {
        columns = Ordered.without(columns, indexOfColumn(event.columnId()));
    }

    // ---------------------------------------------------------------- 查找（不改狀態）

    private int indexOfColumn(ColumnId columnId) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).id().equals(columnId)) {
                return i;
            }
        }
        return -1;
    }
}
