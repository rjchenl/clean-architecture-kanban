package kanban.flowanalysis.entities.boardflow;

/** 工作項目識別碼。 */
public record WorkItemId(String value) {

    public WorkItemId {
        value = Identifiers.check(value);
    }
}
