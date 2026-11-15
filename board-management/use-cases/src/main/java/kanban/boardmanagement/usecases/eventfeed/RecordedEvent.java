package kanban.boardmanagement.usecases.eventfeed;

import kanban.boardmanagement.entities.board.BoardEvent;

/** 已寫入事件紀錄的一筆事件，以及它在全部事件中的位置。 */
public record RecordedEvent(long position, BoardEvent event) {
}
