package kanban.flowanalysis.entities.boardflow;

/** 流程欄位在現有欄位中的位置，從 0 起算。值來自看板管理。 */
public record FlowColumnPosition(int index) {

    public FlowColumnPosition {
        if (index < 0) {
            throw new IllegalArgumentException("欄位位置從 0 起算，收到 " + index);
        }
    }
}
