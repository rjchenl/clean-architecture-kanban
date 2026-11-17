package kanban.flowanalysis.usecases.commands.checkstuckworkitems;

import kanban.flowanalysis.usecases.commands.CommandResponse;

public interface CheckStuckWorkItemsOutput {

    void present(CommandResponse response);
}
