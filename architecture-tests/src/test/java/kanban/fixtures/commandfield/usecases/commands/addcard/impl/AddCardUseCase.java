package kanban.fixtures.commandfield.usecases.commands.addcard.impl;

import java.util.function.IntSupplier;
import kanban.fixtures.commandfield.usecases.commands.addcard.AddCardOutput;
import kanban.fixtures.commandfield.usecases.commands.gateway.BoardRepository;

/** 違規樣本（CQRS-4）：指令 Use Case 多拿了一個不在允許清單上的協作者（這裡用 JDK 的函式介面偽裝）。 */
public final class AddCardUseCase {
    BoardRepository boards;
    AddCardOutput output;
    IntSupplier cardCountFromReadModel;
}
