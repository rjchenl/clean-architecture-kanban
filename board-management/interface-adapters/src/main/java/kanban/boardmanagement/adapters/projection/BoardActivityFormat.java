package kanban.boardmanagement.adapters.projection;

import java.util.Optional;
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
import kanban.boardmanagement.usecases.eventfeed.RecordedEvent;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind;

/**
 * 把看板管理的領域事件換成公布格式。只是格式轉換，沒有業務判斷（DDD-9）。
 *
 * <p>對全部事件逐一列出、不寫 default：看板管理新增一種事件時這裡會編譯失敗，
 * 逼人明確決定公布或不公布。欄內排序、在製品上限目前沒有下游需要，不公布（設計第 4.4 節）。
 */
final class BoardActivityFormat {

    private BoardActivityFormat() {
    }

    static Optional<BoardActivity> of(RecordedEvent recorded) {
        long position = recorded.position();
        return switch (recorded.event()) {
            case BoardCreated e -> published(position, BoardActivityKind.BOARD_CREATED, e,
                    e.name().value(), null, null, null, null, null, null, null);
            case BoardRenamed e -> published(position, BoardActivityKind.BOARD_RENAMED, e,
                    e.name().value(), null, null, null, null, null, null, null);
            case BoardArchived e -> published(position, BoardActivityKind.BOARD_ARCHIVED, e,
                    null, null, null, null, null, null, null, null);
            case ColumnAdded e -> published(position, BoardActivityKind.COLUMN_ADDED, e,
                    null, e.columnId().value(), e.name().value(), null, null, null, null, null);
            case ColumnRenamed e -> published(position, BoardActivityKind.COLUMN_RENAMED, e,
                    null, e.columnId().value(), e.name().value(), null, null, null, null, null);
            case ColumnMoved e -> published(position, BoardActivityKind.COLUMN_MOVED, e,
                    null, e.columnId().value(), null, e.toPosition().index(), null, null, null, null);
            case ColumnRemoved e -> published(position, BoardActivityKind.COLUMN_REMOVED, e,
                    null, e.columnId().value(), null, null, null, null, null, null);
            case CardAdded e -> published(position, BoardActivityKind.CARD_ADDED, e,
                    null, e.columnId().value(), null, null, e.cardId().value(), e.title().value(), null, null);
            case CardContentChanged e -> published(position, BoardActivityKind.CARD_CONTENT_CHANGED, e,
                    null, null, null, null, e.cardId().value(), e.title().value(), null, null);
            case CardMoved e -> published(position, BoardActivityKind.CARD_MOVED, e,
                    null, null, null, null, e.cardId().value(), null,
                    e.fromColumnId().value(), e.toColumnId().value());
            case CardRemoved e -> published(position, BoardActivityKind.CARD_REMOVED, e,
                    null, e.columnId().value(), null, null, e.cardId().value(), null, null, null);
            case CardRepositioned e -> Optional.empty();
            case ColumnWipLimitSet e -> Optional.empty();
            case ColumnWipLimitRemoved e -> Optional.empty();
        };
    }

    private static Optional<BoardActivity> published(long position, BoardActivityKind kind, BoardEvent event,
            String boardName, String columnId, String columnName, Integer columnPosition,
            String cardId, String cardTitle, String fromColumnId, String toColumnId) {
        return Optional.of(new BoardActivity(position, kind, event.boardId().value(), event.occurredAt(),
                boardName, columnId, columnName, columnPosition, cardId, cardTitle, fromColumnId, toColumnId));
    }
}
