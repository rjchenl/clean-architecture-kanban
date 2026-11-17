package kanban.flowanalysis.entities.boardflow;

/** 流程欄位名稱，顯示用。 */
public record FlowColumnName(String value) {

    public FlowColumnName {
        value = DisplayText.of(value, "欄位名稱");
    }
}
