package kanban.fixtures.packagecycles.boardmanagement.usecases.commands.first;

import kanban.fixtures.packagecycles.boardmanagement.usecases.commands.second.Second;

/** 違規樣本（CA-12）：同一圈（Use Cases）裡的兩個指令套件互相引用。 */
public final class First {
    Second second;
}
