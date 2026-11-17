package kanban.flowanalysis.entities.boardflow;

/** 看板流程識別碼。值和看板管理的看板識別碼相同：兩個情境只用識別碼互相參照（DDD-5）。 */
public record BoardFlowId(String value) {

    public BoardFlowId {
        value = Identifiers.check(value);
    }
}
