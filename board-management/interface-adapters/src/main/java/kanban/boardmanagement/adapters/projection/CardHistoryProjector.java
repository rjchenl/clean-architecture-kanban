package kanban.boardmanagement.adapters.projection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import kanban.boardmanagement.entities.board.BoardEvent;
import kanban.boardmanagement.entities.board.CardAdded;
import kanban.boardmanagement.entities.board.CardContentChanged;
import kanban.boardmanagement.entities.board.CardMoved;
import kanban.boardmanagement.entities.board.CardRemoved;
import kanban.boardmanagement.usecases.queries.viewboard.BoardSnapshot;
import kanban.boardmanagement.usecases.queries.viewboard.ColumnSnapshot;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistory;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryEntry;
import kanban.boardmanagement.usecases.queries.viewcardhistory.CardHistoryEntryKind;

/**
 * 把一筆事件抄進「卡片移動歷程」讀取模型。純函式。
 * 欄位名稱取事件發生當下（套用這筆事件之前）看板上的名稱，所以之後欄位改名或被刪，舊歷程不變。
 */
final class CardHistoryProjector {

    private CardHistoryProjector() {
    }

    /** @return 這筆事件讓某張卡片的歷程變成什麼；與卡片歷程無關的事件回傳 empty */
    static Optional<CardHistory> apply(CardHistory current, BoardSnapshot boardBeforeEvent, BoardEvent event) {
        return switch (event) {
            case CardAdded e -> Optional.of(new CardHistory(e.cardId().value(), e.title().value(), List.of(
                    entry(CardHistoryEntryKind.ADDED, null, columnName(boardBeforeEvent, e.columnId().value()),
                            e.occurredAt()))));
            case CardMoved e -> Optional.of(appended(current, entry(CardHistoryEntryKind.MOVED,
                    columnName(boardBeforeEvent, e.fromColumnId().value()),
                    columnName(boardBeforeEvent, e.toColumnId().value()), e.occurredAt())));
            case CardRemoved e -> Optional.of(appended(current, entry(CardHistoryEntryKind.REMOVED,
                    columnName(boardBeforeEvent, e.columnId().value()), null, e.occurredAt())));
            case CardContentChanged e ->
                    Optional.of(new CardHistory(current.cardId(), e.title().value(), current.entries()));
            default -> Optional.empty();
        };
    }

    /** 這筆事件影響的是哪一張卡片的歷程；無關的事件回傳 empty。 */
    static Optional<String> cardIdOf(BoardEvent event) {
        return switch (event) {
            case CardAdded e -> Optional.of(e.cardId().value());
            case CardMoved e -> Optional.of(e.cardId().value());
            case CardRemoved e -> Optional.of(e.cardId().value());
            case CardContentChanged e -> Optional.of(e.cardId().value());
            default -> Optional.empty();
        };
    }

    private static CardHistoryEntry entry(CardHistoryEntryKind kind, String from, String to, Instant at) {
        return new CardHistoryEntry(kind, from, to, at);
    }

    private static CardHistory appended(CardHistory history, CardHistoryEntry entry) {
        List<CardHistoryEntry> entries = new ArrayList<>(history.entries());
        entries.add(entry);
        return new CardHistory(history.cardId(), history.title(), List.copyOf(entries));
    }

    private static String columnName(BoardSnapshot board, String columnId) {
        for (ColumnSnapshot column : board.columns()) {
            if (column.columnId().equals(columnId)) {
                return column.name();
            }
        }
        throw new IllegalStateException("讀取模型裡找不到欄位 " + columnId);
    }
}
