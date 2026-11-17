package kanban.flowanalysis.entities.boardflow;

/** 流程欄位識別碼。 */
public record FlowColumnId(String value) {

    public FlowColumnId {
        value = Identifiers.check(value);
    }
}
