package kanban.flowanalysis.entities.boardflow;

/** 看板流程名稱（看板的名稱），顯示用。 */
public record FlowBoardName(String value) {

    public FlowBoardName {
        value = DisplayText.of(value, "看板名稱");
    }
}
