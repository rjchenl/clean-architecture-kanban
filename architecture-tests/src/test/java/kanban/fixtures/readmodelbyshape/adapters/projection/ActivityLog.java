package kanban.fixtures.readmodelbyshape.adapters.projection;

import kanban.fixtures.readmodelbyshape.usecases.queries.viewthing.ThingReader;

/** 讀取模型的儲存，但名字不叫 ReadModelStore，寫入方法也不在舊的名單裡。 */
public interface ActivityLog extends ThingReader {

    void append(String thing);
}
