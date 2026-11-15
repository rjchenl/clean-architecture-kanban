package kanban.boardmanagement.frameworks.web;

/** 各端點的請求內容形狀。只是 JSON 的容器，不含任何檢查。 */
final class RequestBodies {

    private RequestBodies() {
    }

    record NameBody(String name) {
    }

    record PositionBody(Integer position) {
    }

    record LimitBody(Integer limit) {
    }

    record NewCardBody(String columnId, String title, String description) {
    }

    record CardContentBody(String title, String description) {
    }

    record CardColumnBody(String columnId, Integer position) {
    }
}
