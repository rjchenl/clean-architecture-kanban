package kanban.boardmanagement.adapters.probe;

import kanban.boardmanagement.usecases.probe.ProbeInput;

/** 骨架驗證用的探針，Task 7 移除。 */
public final class ProbeController {

    private final ProbeInput input;

    public ProbeController(ProbeInput input) {
        this.input = input;
    }

    public String handle(String value) {
        return input.execute(value).value();
    }
}
