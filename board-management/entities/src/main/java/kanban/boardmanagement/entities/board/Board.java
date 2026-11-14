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

    // ---------------------------------------------------------------- 指令：卡片

    /** 新增卡片，加在該欄最下面（R9）；會超過在製品上限就拒絕（R2）。 */
    public void addCard(CardId cardId, ColumnId columnId, CardTitle title, CardDescription description, Instant at) {
        requireNotArchived();
        Column column = requireColumn(columnId);
        requireCardIdUnused(cardId);
        requireRoomForOneMore(column);
        record(new CardAdded(id, cardId, columnId, title, description, at));
    }

    public void changeCardContent(CardId cardId, CardTitle title, CardDescription description, Instant at) {
        requireNotArchived();
        Card card = requireCard(cardId);
        if (card.title().equals(title) && card.description().equals(description)) {
            return;
        }
        record(new CardContentChanged(id, cardId, title, description, at));
    }

    /** 把卡片換到另一欄；目標欄就是目前所在欄時，視為欄內排序。 */
    public void moveCard(CardId cardId, ColumnId toColumnId, Position toPosition, Instant at) {
        requireNotArchived();
        requireCard(cardId);
        Column from = columnHolding(cardId);
        Column to = requireColumn(toColumnId);
        if (from.id().equals(to.id())) {
            repositionCard(cardId, toPosition, at);
            return;
        }
        requireWithin(toPosition, to.cardCount());
        requireRoomForOneMore(to);
        record(new CardMoved(id, cardId, from.id(), to.id(), toPosition, at));
    }

    /** 欄內排序：張數不變，不受在製品上限影響（R3）。 */
    public void repositionCard(CardId cardId, Position toPosition, Instant at) {
        requireNotArchived();
        requireCard(cardId);
        Column column = columnHolding(cardId);
        requireWithin(toPosition, column.cardCount() - 1);
        if (column.indexOfCard(cardId) == toPosition.index()) {
            return;
        }
        record(new CardRepositioned(id, cardId, column.id(), toPosition, at));
    }

    public void removeCard(CardId cardId, Instant at) {
        requireNotArchived();
        requireCard(cardId);
        record(new CardRemoved(id, cardId, columnHolding(cardId).id(), at));
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

    private Card requireCard(CardId cardId) {
        Column column = columnHolding(cardId);
        if (column == null) {
            throw new BusinessRuleViolation(RejectionReason.CARD_NOT_FOUND);
        }
        return column.cards().get(column.indexOfCard(cardId));
    }

    private void requireCardIdUnused(CardId cardId) {
        if (columnHolding(cardId) != null) {
            throw new IllegalArgumentException("card id already used: " + cardId.value());
        }
    }

    /** R2：再放一張會不會超過這一欄的在製品上限。 */
    private void requireRoomForOneMore(Column column) {
        if (column.wipLimit() != null && column.cardCount() + 1 > column.wipLimit().value()) {
            throw new BusinessRuleViolation(RejectionReason.WIP_LIMIT_EXCEEDED);
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
            case CardAdded e -> onCardAdded(e);
            case CardContentChanged e -> onCardContentChanged(e);
            case CardMoved e -> onCardMoved(e);
            case CardRepositioned e -> onCardRepositioned(e);
            case CardRemoved e -> onCardRemoved(e);
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

    private void onCardAdded(CardAdded event) {
        int index = indexOfColumn(event.columnId());
        Column column = columns.get(index);
        Card card = new Card(event.cardId(), event.title(), event.description());
        columns = Ordered.replaced(columns, index, column.withCards(Ordered.appended(column.cards(), card)));
    }

    private void onCardContentChanged(CardContentChanged event) {
        Column column = columnHolding(event.cardId());
        int cardIndex = column.indexOfCard(event.cardId());
        Card changed = new Card(event.cardId(), event.title(), event.description());
        columns = Ordered.replaced(columns, columns.indexOf(column),
                column.withCards(Ordered.replaced(column.cards(), cardIndex, changed)));
    }

    private void onCardMoved(CardMoved event) {
        int fromIndex = indexOfColumn(event.fromColumnId());
        Column from = columns.get(fromIndex);
        int cardIndex = from.indexOfCard(event.cardId());
        Card card = from.cards().get(cardIndex);
        columns = Ordered.replaced(columns, fromIndex, from.withCards(Ordered.without(from.cards(), cardIndex)));

        int toIndex = indexOfColumn(event.toColumnId());
        Column to = columns.get(toIndex);
        columns = Ordered.replaced(columns, toIndex,
                to.withCards(Ordered.inserted(to.cards(), event.toPosition().index(), card)));
    }

    private void onCardRepositioned(CardRepositioned event) {
        int index = indexOfColumn(event.columnId());
        Column column = columns.get(index);
        columns = Ordered.replaced(columns, index, column.withCards(
                Ordered.moved(column.cards(), column.indexOfCard(event.cardId()), event.toPosition().index())));
    }

    private void onCardRemoved(CardRemoved event) {
        int index = indexOfColumn(event.columnId());
        Column column = columns.get(index);
        columns = Ordered.replaced(columns, index,
                column.withCards(Ordered.without(column.cards(), column.indexOfCard(event.cardId()))));
    }

    // ---------------------------------------------------------------- 查找（不改狀態）

    /** 這張卡片目前在哪一欄；找不到回傳 null。 */
    private Column columnHolding(CardId cardId) {
        for (Column column : columns) {
            if (column.indexOfCard(cardId) >= 0) {
                return column;
            }
        }
        return null;
    }

    private int indexOfColumn(ColumnId columnId) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).id().equals(columnId)) {
                return i;
            }
        }
        return -1;
    }
}
