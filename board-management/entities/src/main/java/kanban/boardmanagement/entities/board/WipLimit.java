package kanban.boardmanagement.entities.board;

/** 在製品上限：一欄最多能同時放幾張卡片。必須是 1 以上。 */
public record WipLimit(int value) {

    public WipLimit {
        if (value < 1) {
            throw new BusinessRuleViolation(RejectionReason.WIP_LIMIT_NOT_POSITIVE);
        }
    }
}
