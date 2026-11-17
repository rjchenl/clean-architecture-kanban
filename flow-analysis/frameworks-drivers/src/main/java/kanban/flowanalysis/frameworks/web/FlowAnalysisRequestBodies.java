package kanban.flowanalysis.frameworks.web;

/** 流程分析各端點的請求內容形狀。只是 JSON 的容器，不含任何檢查。 */
final class FlowAnalysisRequestBodies {

    private FlowAnalysisRequestBodies() {
    }

    record StageBody(String stage) {
    }

    record StuckThresholdBody(Integer days) {
    }
}
