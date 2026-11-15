package kanban.boardmanagement.entities.board;

/** 位置：欄位在看板上、或卡片在欄內排第幾個。從 0 起算。 */
public record Position(int index) {

    public Position {
        if (index < 0) {
            throw new BusinessRuleViolation(RejectionReason.POSITION_OUT_OF_RANGE);
        }
    }
}
