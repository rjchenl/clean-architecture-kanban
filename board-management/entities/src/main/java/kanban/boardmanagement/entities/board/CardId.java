package kanban.boardmanagement.entities.board;

/** 卡片識別碼。不可空白。 */
public record CardId(String value) {

    public CardId {
        value = Text.identifier(value);
    }
}
