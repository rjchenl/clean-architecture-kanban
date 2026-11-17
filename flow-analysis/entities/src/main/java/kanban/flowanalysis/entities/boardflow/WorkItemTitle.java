package kanban.flowanalysis.entities.boardflow;

/** 工作項目標題，顯示用。 */
public record WorkItemTitle(String value) {

    public WorkItemTitle {
        value = DisplayText.of(value, "工作項目標題");
    }
}
