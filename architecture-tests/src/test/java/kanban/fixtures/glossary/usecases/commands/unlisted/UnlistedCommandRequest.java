package kanban.fixtures.glossary.usecases.commands.unlisted;

/** 違規樣本（DDD-1）：不在用語表裡的指令。 */
public record UnlistedCommandRequest(String boardId) {
}
