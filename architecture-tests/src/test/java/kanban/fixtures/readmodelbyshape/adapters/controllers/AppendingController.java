package kanban.fixtures.readmodelbyshape.adapters.controllers;

import kanban.fixtures.readmodelbyshape.adapters.projection.ActivityLog;

/** 違規樣本：Controller 直接寫讀取模型（CQRS-5）。 */
public class AppendingController {

    private final ActivityLog log;

    public AppendingController(ActivityLog log) {
        this.log = log;
    }

    public void handle() {
        log.append("thing");
    }
}
