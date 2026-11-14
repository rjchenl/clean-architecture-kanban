package kanban.fixtures.commandreadsreadmodel.usecases.commands.movecard.impl;

import kanban.fixtures.commandreadsreadmodel.usecases.queries.viewboard.BoardSnapshotReader;

/** 違規樣本（CQRS-4）：指令 Use Case 去讀取模型數張數來判斷有沒有超過上限。 */
public final class MoveCardUseCase {
    BoardSnapshotReader readModel;
}
