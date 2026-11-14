package kanban.fixtures.eventnaming.entities;

/** 違規樣本（DDD-7）：事件用命令語氣命名，不是過去式。 */
public record CreateBoard(String boardId) implements SomethingEvent {
}
