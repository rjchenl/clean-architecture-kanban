package kanban.boardmanagement.usecases.probe;

import kanban.boardmanagement.entities.probe.EntitiesProbe;

/** 骨架驗證用的探針，Task 7 移除。 */
public interface ProbeInput {

    EntitiesProbe execute(String value);
}
