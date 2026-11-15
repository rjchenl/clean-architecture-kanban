package kanban.boardmanagement.entities.board;

/** 欄位識別碼。不可空白。 */
public record ColumnId(String value) {

    public ColumnId {
        value = Text.identifier(value);
    }
}
