package kanban.boardmanagement.entities.board;

/** 欄位名稱。不可空白，去掉頭尾空白後最長 100 字。 */
public record ColumnName(String value) {

    public ColumnName {
        value = Text.name(value);
    }
}
