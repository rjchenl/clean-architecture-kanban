package kanban.boardmanagement.adapters.projection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import kanban.boardmanagement.entities.board.BoardArchived;
import kanban.boardmanagement.entities.board.BoardCreated;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.BoardRenamed;
import kanban.boardmanagement.entities.board.CardAdded;
import kanban.boardmanagement.entities.board.CardContentChanged;
import kanban.boardmanagement.entities.board.CardMoved;
import kanban.boardmanagement.entities.board.CardRemoved;
import kanban.boardmanagement.entities.board.CardRepositioned;
import kanban.boardmanagement.entities.board.ColumnAdded;
import kanban.boardmanagement.entities.board.ColumnMoved;
import kanban.boardmanagement.entities.board.ColumnRemoved;
import kanban.boardmanagement.entities.board.ColumnRenamed;
import kanban.boardmanagement.entities.board.ColumnWipLimitRemoved;
import kanban.boardmanagement.entities.board.ColumnWipLimitSet;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.CardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.ColumnSnapshot;

/**
 * 把一筆事件抄進「看板現況」讀取模型。純函式，沒有任何業務判斷（DDD-9）：
 * 事件是已經發生的事實，這裡只照抄，不重新檢查能不能做。
 */
final class BoardSnapshotProjector {

    private BoardSnapshotProjector() {
    }

    static BoardSnapshot apply(BoardSnapshot current, BoardEvent event) {
        return switch (event) {
            case BoardCreated e -> new BoardSnapshot(e.boardId().value(), e.name().value(), false, List.of());
            case BoardRenamed e ->
                    new BoardSnapshot(current.boardId(), e.name().value(), current.archived(), current.columns());
            case BoardArchived e -> new BoardSnapshot(current.boardId(), current.name(), true, current.columns());
            case ColumnAdded e -> withColumns(current, columns ->
                    columns.add(new ColumnSnapshot(e.columnId().value(), e.name().value(), null, List.of())));
            case ColumnRenamed e -> withColumn(current, e.columnId().value(), column ->
                    new ColumnSnapshot(column.columnId(), e.name().value(), column.wipLimit(), column.cards()));
            case ColumnMoved e -> withColumns(current, columns ->
                    columns.add(e.toPosition().index(), columns.remove(indexOfColumn(columns, e.columnId().value()))));
            case ColumnWipLimitSet e -> withColumn(current, e.columnId().value(), column ->
                    new ColumnSnapshot(column.columnId(), column.name(), e.wipLimit().value(), column.cards()));
            case ColumnWipLimitRemoved e -> withColumn(current, e.columnId().value(), column ->
                    new ColumnSnapshot(column.columnId(), column.name(), null, column.cards()));
            case ColumnRemoved e -> withColumns(current, columns ->
                    columns.remove(indexOfColumn(columns, e.columnId().value())));
            case CardAdded e -> withCards(current, e.columnId().value(), cards ->
                    cards.add(new CardSnapshot(e.cardId().value(), e.title().value(), e.description().value())));
            case CardContentChanged e -> withColumns(current, columns -> {
                for (int i = 0; i < columns.size(); i++) {
                    int cardIndex = indexOfCard(columns.get(i).cards(), e.cardId().value());
                    if (cardIndex >= 0) {
                        List<CardSnapshot> cards = new ArrayList<>(columns.get(i).cards());
                        cards.set(cardIndex,
                                new CardSnapshot(e.cardId().value(), e.title().value(), e.description().value()));
                        columns.set(i, withCards(columns.get(i), cards));
                    }
                }
            });
            case CardMoved e -> withColumns(current, columns -> {
                int from = indexOfColumn(columns, e.fromColumnId().value());
                List<CardSnapshot> source = new ArrayList<>(columns.get(from).cards());
                CardSnapshot card = source.remove(indexOfCard(source, e.cardId().value()));
                columns.set(from, withCards(columns.get(from), source));

                int to = indexOfColumn(columns, e.toColumnId().value());
                List<CardSnapshot> target = new ArrayList<>(columns.get(to).cards());
                target.add(e.toPosition().index(), card);
                columns.set(to, withCards(columns.get(to), target));
            });
            case CardRepositioned e -> withCards(current, e.columnId().value(), cards ->
                    cards.add(e.toPosition().index(), cards.remove(indexOfCard(cards, e.cardId().value()))));
            case CardRemoved e -> withCards(current, e.columnId().value(), cards ->
                    cards.remove(indexOfCard(cards, e.cardId().value())));
        };
    }

    /** 可變清單上的一個修改動作。 */
    private interface Edit<T> {
        void on(List<T> items);
    }

    private static BoardSnapshot withColumns(BoardSnapshot board, Edit<ColumnSnapshot> edit) {
        List<ColumnSnapshot> columns = new ArrayList<>(board.columns());
        edit.on(columns);
        return new BoardSnapshot(board.boardId(), board.name(), board.archived(), List.copyOf(columns));
    }

    private static BoardSnapshot withColumn(BoardSnapshot board, String columnId, UnaryOperator<ColumnSnapshot> change) {
        return withColumns(board, columns -> {
            int index = indexOfColumn(columns, columnId);
            columns.set(index, change.apply(columns.get(index)));
        });
    }

    private static BoardSnapshot withCards(BoardSnapshot board, String columnId, Edit<CardSnapshot> edit) {
        return withColumn(board, columnId, column -> {
            List<CardSnapshot> cards = new ArrayList<>(column.cards());
            edit.on(cards);
            return withCards(column, cards);
        });
    }

    private static ColumnSnapshot withCards(ColumnSnapshot column, List<CardSnapshot> cards) {
        return new ColumnSnapshot(column.columnId(), column.name(), column.wipLimit(), List.copyOf(cards));
    }

    private static int indexOfColumn(List<ColumnSnapshot> columns, String columnId) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).columnId().equals(columnId)) {
                return i;
            }
        }
        throw new IllegalStateException("讀取模型裡找不到欄位 " + columnId);
    }

    private static int indexOfCard(List<CardSnapshot> cards, String cardId) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).cardId().equals(cardId)) {
                return i;
            }
        }
        return -1;
    }
}
