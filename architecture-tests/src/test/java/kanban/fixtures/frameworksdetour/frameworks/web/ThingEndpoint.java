package kanban.fixtures.frameworksdetour.frameworks.web;

import kanban.fixtures.frameworksdetour.frameworks.api.MetricsHelper;

/** 端點本身只相依同一圈的另一個套件，看起來合規；資料其實是 MetricsHelper 從 Use Cases 圈直接拿的。 */
public final class ThingEndpoint {

    private final MetricsHelper helper;

    public ThingEndpoint(MetricsHelper helper) {
        this.helper = helper;
    }

    public String view(String id) {
        return helper.find(id);
    }
}
