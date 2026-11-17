package kanban.fixtures.contextmapleak.downstream.adapters.translation;

import kanban.fixtures.contextmapleak.upstream.usecases.commands.InternalCommand;

/** 違規樣本：防腐層引用上游公布格式以外的東西（設計第 0 節 4a：編譯器擋不到，只能靠這條規則）。 */
public class UsesInternal {

    InternalCommand command;
}
