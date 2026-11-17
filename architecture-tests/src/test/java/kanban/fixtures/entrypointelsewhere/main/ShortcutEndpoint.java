package kanban.fixtures.entrypointelsewhere.main;

import org.springframework.web.bind.annotation.RestController;

/** 違規樣本（CA-7）：HTTP 端點放在 Main，躲開「HTTP 薄殼只轉手」（Main 不受圈的限制）。 */
@RestController
public final class ShortcutEndpoint {
}
