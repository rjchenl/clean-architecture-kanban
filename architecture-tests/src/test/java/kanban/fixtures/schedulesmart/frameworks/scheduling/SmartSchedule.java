package kanban.fixtures.schedulesmart.frameworks.scheduling;

import kanban.fixtures.schedulesmart.usecases.commands.checkstuckworkitems.CheckStuckWorkItemsInput;

/** 違規樣本（CA-7）：背景排程直接呼叫 Use Case，自己決定要檢查哪些看板。 */
public final class SmartSchedule {

    CheckStuckWorkItemsInput check;

    void checkStuck() {
        check.execute("board-1");
    }
}
