package kanban.flowanalysis.entities.boardflow;

/**
 * 流程欄位的階段（F1）。新出現的欄位一律是未分類（F2）；未分類的欄位不影響任何開始或完成的判斷。
 */
public enum Stage {
    UNCLASSIFIED,
    NOT_STARTED,
    IN_PROGRESS,
    DONE;

    /** 依名稱取得階段；使用者送來不認得的名稱時拒絕。 */
    public static Stage named(String name) {
        for (Stage stage : values()) {
            if (stage.name().equals(name)) {
                return stage;
            }
        }
        throw new BusinessRuleViolation(RejectionReason.UNKNOWN_STAGE);
    }
}
