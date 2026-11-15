package kanban.boardmanagement.entities.board;

/** 卡片標題。不可空白，去掉頭尾空白後最長 100 字。 */
public record CardTitle(String value) {

    public CardTitle {
        value = Text.name(value);
    }
}
