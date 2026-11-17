package kanban.fixtures.frameworksdetour.frameworks.api;

import kanban.fixtures.frameworksdetour.usecases.queries.viewthing.ThingReader;

/** 違規樣本（CA-7，2026-11-17 獨立審查 I-1）：最外圈另開一個套件直接讀 Use Cases 圈，繞過 web／scheduling 的限制。 */
public final class MetricsHelper {

    private final ThingReader reader;

    public MetricsHelper(ThingReader reader) {
        this.reader = reader;
    }

    public String find(String id) {
        return reader.find(id);
    }
}
