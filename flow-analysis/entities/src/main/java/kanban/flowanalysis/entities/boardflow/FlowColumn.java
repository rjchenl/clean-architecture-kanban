package kanban.flowanalysis.entities.boardflow;

/**
 * 流程欄位：看板流程的內部物件（DDD-4，不公開）。看板上的一欄，在流程分析裡多一個階段。
 * 刪掉後保留刪除前的階段，用來判斷曾經過那一欄的歷史（F12）。
 */
record FlowColumn(FlowColumnId id, FlowColumnName name, Stage stage, boolean removed) {

    FlowColumn withName(FlowColumnName newName) {
        return new FlowColumn(id, newName, stage, removed);
    }

    FlowColumn withStage(Stage newStage) {
        return new FlowColumn(id, name, newStage, removed);
    }

    FlowColumn removedNow() {
        return new FlowColumn(id, name, stage, true);
    }
}
