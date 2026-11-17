package kanban.flowanalysis.usecases.commands.setstuckthreshold;

import kanban.flowanalysis.usecases.commands.CommandResponse;

public interface SetStuckThresholdOutput {

    void present(CommandResponse response);
}
