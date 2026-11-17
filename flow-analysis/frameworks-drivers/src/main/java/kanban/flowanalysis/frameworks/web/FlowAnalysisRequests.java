package kanban.flowanalysis.frameworks.web;

import kanban.flowanalysis.adapters.presenters.CommandViewModel;
import kanban.flowanalysis.adapters.presenters.FlowMetricsViewModel;

/**
 * 流程分析的 HTTP 薄殼要的東西：每個請求對應一個方法，收原始輸入、回傳整理好的 ViewModel。
 * 介面定義在這一圈，由 Main 實作——薄殼不認識 Use Case、Controller、Presenter 怎麼接（CA-8）。
 */
public interface FlowAnalysisRequests {

    /** 流程分析頁（看板流程的識別碼＝看板的識別碼）。 */
    FlowMetricsViewModel viewFlowMetrics(String boardFlowId);

    /** 把一個欄位歸到某個階段。 */
    CommandViewModel setColumnStage(String boardFlowId, String flowColumnId, String stage);

    /** 設定卡住門檻（天）。 */
    CommandViewModel setStuckThreshold(String boardFlowId, Integer days);

    /** 取消卡住門檻。 */
    CommandViewModel removeStuckThreshold(String boardFlowId);

    /** 請求內容根本讀不懂（例如不是合法的 JSON）時要回覆的內容。訊息文字仍然出自 Presenter。 */
    CommandViewModel invalidRequest();
}
