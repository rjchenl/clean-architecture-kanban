package kanban.boardmanagement.entities.board;

/** 看板識別碼。不可空白。 */
public record BoardId(String value) {

    public BoardId {
        value = Text.identifier(value);
    }
}
