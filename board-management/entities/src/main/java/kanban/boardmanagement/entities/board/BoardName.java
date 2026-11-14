package kanban.boardmanagement.entities.board;

/** 看板名稱。不可空白，去掉頭尾空白後最長 100 字。 */
public record BoardName(String value) {

    public BoardName {
        value = Text.name(value);
    }
}
