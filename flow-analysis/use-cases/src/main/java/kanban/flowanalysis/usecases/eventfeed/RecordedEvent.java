package kanban.flowanalysis.usecases.eventfeed;

import kanban.flowanalysis.entities.boardflow.BoardFlowEvent;

/** 事件紀錄裡的一筆事件與它的全域位置。 */
public record RecordedEvent(long position, BoardFlowEvent event) {
}
