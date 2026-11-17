package kanban.flowanalysis.entities.boardflow;

/** 流程分析的指令被拒絕時，違反的是哪一條規則。看板上發生的事實一律接受，不會出現在這裡（F14）。 */
public enum RejectionReason {
    IDENTIFIER_BLANK,
    INVALID_CHARACTERS,
    UNKNOWN_STAGE,
    STUCK_THRESHOLD_OUT_OF_RANGE,
    FLOW_COLUMN_NOT_FOUND,
    FLOW_COLUMN_REMOVED
}
