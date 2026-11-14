package kanban.boardmanagement.entities.board;

/** 卡片說明。可以空白，最長 2000 字。 */
public record CardDescription(String value) {

    public CardDescription {
        value = Text.description(value);
    }
}
