package kanban.boardmanagement.entities.board;

/** 違反業務規則：指令因此被拒絕。只有領域模型自己能建立。 */
public final class BusinessRuleViolation extends RuntimeException {

    private final RejectionReason reason;

    BusinessRuleViolation(RejectionReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public RejectionReason reason() {
        return reason;
    }
}
