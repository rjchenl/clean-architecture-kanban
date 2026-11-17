package kanban.main;

import java.util.ArrayList;
import java.util.List;
import kanban.flowanalysis.frameworks.scheduling.FlowAnalysisBackgroundWork;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** 流程分析整合測試共用：背景排程在測試裡關掉，要同步時直接呼叫第 4 圈的背景工作介面。 */
abstract class FlowAnalysisTestSupport extends HttpTestSupport {

    @Autowired
    FlowAnalysisBackgroundWork work;

    /** 同步一次看板動態（背景排程每 2 秒做的事）。 */
    void sync() {
        work.syncBoardActivities();
    }

    Reply moveCard(String boardId, String cardId, String columnId) {
        return put("/api/boards/" + boardId + "/cards/" + cardId + "/column",
                "{\"columnId\":\"" + columnId + "\",\"position\":0}");
    }

    Reply viewFlow(String boardId) {
        return get("/api/flow-analysis/boards/" + boardId);
    }

    static String stagePath(String boardId, String columnId) {
        return "/api/flow-analysis/boards/" + boardId + "/columns/" + columnId + "/stage";
    }

    static String thresholdPath(String boardId) {
        return "/api/flow-analysis/boards/" + boardId + "/stuck-threshold";
    }

    Reply setStage(String boardId, String columnId, String stage) {
        return put(stagePath(boardId, columnId), "{\"stage\":\"" + stage + "\"}");
    }

    Reply setThreshold(String boardId, int days) {
        return put(thresholdPath(boardId), "{\"days\":" + days + "}");
    }

    /** 各欄「名稱:張數」。 */
    static List<String> columns(Reply view) {
        List<String> columns = new ArrayList<>();
        for (JsonNode column : view.body().get("columns")) {
            columns.add(column.get("name").asString() + ":" + column.get("workItemCount").asString());
        }
        return columns;
    }

    /** 卡住清單上的工作項目標題。 */
    static List<String> stuckTitles(Reply view) {
        List<String> titles = new ArrayList<>();
        for (JsonNode item : view.body().get("stuckWorkItems")) {
            titles.add(item.get("title").asString());
        }
        return titles;
    }
}
